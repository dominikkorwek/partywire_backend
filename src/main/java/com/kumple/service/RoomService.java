package com.kumple.service;

import com.kumple.model.Player;
import com.kumple.model.Room;
import com.kumple.repository.RoomRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int DEFAULT_MAX_PLAYERS = 10;

    private final RoomRepository roomRepository;
    private final SecureRandom random = new SecureRandom();

    // WebSocket session tracking stays in-memory (ephemeral data)
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, PresenceInfo> presenceMap = new ConcurrentHashMap<>();

    public static final class SessionInfo {
        private final String roomCode;
        private final String playerId;
        private volatile long lastSeenAt;

        public SessionInfo(String roomCode, String playerId) {
            this.roomCode = roomCode;
            this.playerId = playerId;
            touch();
        }

        public String roomCode() {
            return roomCode;
        }

        public String playerId() {
            return playerId;
        }

        public long lastSeenAt() {
            return lastSeenAt;
        }

        public void touch() {
            this.lastSeenAt = System.currentTimeMillis();
        }
    }
    public static final class PresenceInfo {
        private final String roomCode;
        private volatile long lastSeenAt;

        public PresenceInfo(String roomCode) {
            this.roomCode = roomCode;
            touch();
        }

        public String roomCode() {
            return roomCode;
        }

        public long lastSeenAt() {
            return lastSeenAt;
        }

        public void touch() {
            this.lastSeenAt = System.currentTimeMillis();
        }
    }
    public record DisconnectResult(String roomCode, String playerId, boolean roomClosed, Room room) {}

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Room createRoom(String hostNickname, Integer maxPlayers, String avatarAnimal, String avatarColor, String hostAuthSubject) {
        if (hostAuthSubject != null) {
            roomRepository.findByHostAuthSubject(hostAuthSubject).ifPresent(oldRoom -> {
                sessionMap.values().removeIf(s -> s.roomCode().equalsIgnoreCase(oldRoom.getCode()));
                presenceMap.entrySet().removeIf(e -> e.getValue().roomCode().equalsIgnoreCase(oldRoom.getCode()));
                roomRepository.delete(oldRoom);
            });
        }

        String code = generateCode();
        int max = maxPlayers != null ? maxPlayers : DEFAULT_MAX_PLAYERS;
        Room room = new Room(code, max, hostAuthSubject);
        room.addPlayer(hostNickname, true, avatarAnimal, avatarColor);
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public Optional<Room> getRoom(String code) {
        return roomRepository.findByCodeIgnoreCase(code);
    }

    @Transactional
    public Player joinRoom(String code, String nickname, String avatarAnimal, String avatarColor) {
        Room room = roomRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Pokój o kodzie " + code + " nie istnieje"));

        if (room.isFull()) {
            throw new IllegalStateException("Pokój jest pełny");
        }

        boolean nickTaken = room.getPlayers().stream()
                .anyMatch(p -> p.getNickname().equalsIgnoreCase(nickname));
        if (nickTaken) {
            throw new IllegalArgumentException("Nick \"" + nickname + "\" jest już zajęty w tym pokoju");
        }

        Player player = room.addPlayer(nickname, false, avatarAnimal, avatarColor);
        roomRepository.save(room);
        return player;
    }

    @Transactional
    public boolean leaveRoom(String code, String playerId) {
        Room room = roomRepository.findByCodeIgnoreCase(code).orElse(null);
        if (room == null) return false;

        Player player = room.findByPlayerId(playerId);
        if (player == null) return false;

        if (player.isHost()) {
            sessionMap.values().removeIf(s -> s.roomCode().equalsIgnoreCase(code));
            presenceMap.entrySet().removeIf(e -> e.getValue().roomCode().equalsIgnoreCase(code));
            roomRepository.delete(room);
            return true;
        }

        room.removePlayer(playerId);
        presenceMap.remove(playerId);
        roomRepository.save(room);
        return false;
    }

    @Transactional
    public void closeRoom(String code, String hostAuthSubject) {
        assertHost(code, hostAuthSubject);
        Room room = roomRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Pokój o kodzie " + code + " nie istnieje"));
        sessionMap.values().removeIf(s -> s.roomCode().equalsIgnoreCase(code));
        presenceMap.entrySet().removeIf(e -> e.getValue().roomCode().equalsIgnoreCase(code));
        roomRepository.delete(room);
    }

    public void registerSession(String sessionId, String roomCode, String playerId) {
        sessionMap.put(sessionId, new SessionInfo(roomCode.toUpperCase(), playerId));
    }

    public void touchSession(String sessionId) {
        SessionInfo session = sessionMap.get(sessionId);
        if (session != null) {
            session.touch();
        }
    }

    public void touchPresence(String roomCode, String playerId) {
        if (roomCode == null || roomCode.isBlank() || playerId == null || playerId.isBlank()) {
            return;
        }
        presenceMap.compute(playerId, (key, current) -> {
            if (current == null || !current.roomCode().equalsIgnoreCase(roomCode)) {
                return new PresenceInfo(roomCode.toUpperCase());
            }
            current.touch();
            return current;
        });
    }

    public void unregisterSession(String sessionId) {
        sessionMap.remove(sessionId);
    }

    public Optional<SessionInfo> getSession(String sessionId) {
        return Optional.ofNullable(sessionMap.get(sessionId));
    }

    @Transactional
    public List<DisconnectResult> evictInactiveSessions(long timeoutMs) {
        long cutoff = System.currentTimeMillis() - timeoutMs;
        List<String> expiredSessionIds = sessionMap.entrySet().stream()
                .filter(entry -> entry.getValue().lastSeenAt() < cutoff)
                .map(Map.Entry::getKey)
                .toList();

        List<DisconnectResult> results = new ArrayList<>();
        for (String sessionId : expiredSessionIds) {
            handleSessionDisconnect(sessionId).ifPresent(results::add);
        }
        return results;
    }

    @Transactional
    public List<DisconnectResult> evictInactivePresences(long timeoutMs) {
        long cutoff = System.currentTimeMillis() - timeoutMs;
        List<Map.Entry<String, PresenceInfo>> expiredEntries = presenceMap.entrySet().stream()
                .filter(entry -> entry.getValue().lastSeenAt() < cutoff)
                .toList();

        List<DisconnectResult> results = new ArrayList<>();
        for (Map.Entry<String, PresenceInfo> entry : expiredEntries) {
            String playerId = entry.getKey();
            PresenceInfo removed = presenceMap.remove(playerId);
            if (removed == null) {
                continue;
            }

            boolean roomClosed = leaveRoom(removed.roomCode(), playerId);
            if (roomClosed) {
                results.add(new DisconnectResult(removed.roomCode(), playerId, true, null));
                continue;
            }

            getRoom(removed.roomCode())
                    .map(room -> new DisconnectResult(removed.roomCode(), playerId, false, room))
                    .ifPresent(results::add);
        }
        return results;
    }

    @Transactional
    public Optional<DisconnectResult> handleSessionDisconnect(String sessionId) {
        SessionInfo session = sessionMap.remove(sessionId);
        if (session == null) {
            return Optional.empty();
        }

        boolean hasAnotherActiveSession = sessionMap.entrySet().stream()
                .anyMatch(entry ->
                        !entry.getKey().equals(sessionId)
                                && entry.getValue().roomCode().equalsIgnoreCase(session.roomCode())
                                && entry.getValue().playerId().equals(session.playerId())
                );

        if (hasAnotherActiveSession) {
            return Optional.empty();
        }

        boolean roomClosed = leaveRoom(session.roomCode(), session.playerId());
        if (roomClosed) {
            return Optional.of(new DisconnectResult(session.roomCode(), session.playerId(), true, null));
        }

        return getRoom(session.roomCode())
                .map(room -> new DisconnectResult(session.roomCode(), session.playerId(), false, room));
    }

    @Transactional(readOnly = true)
    public void assertHost(String code, String hostAuthSubject) {
        Room room = roomRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Pokój o kodzie " + code + " nie istnieje"));

        // when OAuth2 is disabled (anonymous mode) both sides are null - allow all host actions
        if (hostAuthSubject == null && room.getHostAuthSubject() == null) return;

        if (hostAuthSubject == null || !hostAuthSubject.equals(room.getHostAuthSubject())) {
            throw new AccessDeniedException("Tylko host tego pokoju może wykonać tę akcję");
        }
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (roomRepository.findByCodeIgnoreCase(code).isPresent());
        return code;
    }
}

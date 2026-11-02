package q.qedet.common.rate;

import org.springframework.stereotype.Service;
import q.qedet.common.exception.TooManyRequestsException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public void requireAllowed(String key, int limit, Duration window, String message) {
        Instant now = Instant.now();
        Deque<Instant> deque = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(now.minus(window))) {
                deque.removeFirst();
            }
            if (deque.size() >= limit) {
                throw new TooManyRequestsException(message);
            }
            deque.addLast(now);
        }
    }
}

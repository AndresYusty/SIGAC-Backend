package com.university.sigac.core.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Generates serial numbers per key (e.g. "MIN-CF-2026"), with locking to avoid duplicates. */
@Service
@RequiredArgsConstructor
public class SequenceService {

    private final SequenceCounterRepository repository;

    @Transactional
    public long next(String key) {
        SequenceCounter c = repository.findByKeyForUpdate(key)
                .orElseGet(() -> repository.saveAndFlush(new SequenceCounter(key, 0)));
        c.setLastValue(c.getLastValue() + 1);
        return c.getLastValue();
    }
}

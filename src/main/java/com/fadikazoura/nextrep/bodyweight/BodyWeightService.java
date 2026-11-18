package com.fadikazoura.nextrep.bodyweight;

import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserNotFoundException;
import com.fadikazoura.nextrep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BodyWeightService {

    private final BodyWeightEntryRepository bodyWeightEntryRepository;
    private final UserRepository userRepository;

    @Transactional
    public BodyWeightResponse logWeight(Long userId, BodyWeightRequest request) {
        LocalDate day = request.recordedOn() != null ? request.recordedOn() : LocalDate.now();

        BodyWeightEntry entry = bodyWeightEntryRepository.findByUserIdAndRecordedOn(userId, day)
                .orElseGet(() -> new BodyWeightEntry(findUser(userId), request.weightKg(), day));
        entry.changeWeight(request.weightKg());

        return BodyWeightResponse.from(bodyWeightEntryRepository.saveAndFlush(entry));
    }

    @Transactional(readOnly = true)
    public List<BodyWeightResponse> getHistory(Long userId) {
        return bodyWeightEntryRepository.findByUserIdOrderByRecordedOnAsc(userId).stream()
                .map(BodyWeightResponse::from)
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}

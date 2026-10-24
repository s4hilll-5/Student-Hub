package com.studenthub.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.common.PageResponse;
import com.studenthub.dto.note.NoteRequest;
import com.studenthub.dto.note.NoteResponse;
import com.studenthub.entity.Note;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.NoteMapper;
import com.studenthub.repository.NoteRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.repository.spec.NoteSpecifications;

@Service
@Transactional(readOnly = true)
public class NoteService {

    static final Set<String> SORTABLE = Set.of("updatedAt", "createdAt", "title");
    static final int MAX_TAGS = 10;

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository, SubjectService subjectService) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.subjectService = subjectService;
    }

    /** Pinned notes always come first; within each group the requested sort applies (default: last edited). */
    public PageResponse<NoteResponse> list(Long userId, String search, Long subjectId, String tag, Boolean pinned,
            Pageable pageable) {
        Pageable sanitised = PageRequests.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Pageable page = PageRequest.of(sanitised.getPageNumber(), sanitised.getPageSize(),
                Sort.by(Sort.Direction.DESC, "pinned").and(sanitised.getSort()));
        Page<Note> result = noteRepository.findAll(
                NoteSpecifications.forFilter(userId, search, subjectId, tag, pinned), page);
        return PageResponse.of(result, NoteMapper::toResponse);
    }

    public List<String> tags(Long userId) {
        return noteRepository.findDistinctTagsByUserId(userId);
    }

    public NoteResponse get(Long userId, Long id) {
        return NoteMapper.toResponse(requireOwned(userId, id));
    }

    @Transactional
    public NoteResponse create(Long userId, NoteRequest request) {
        Note note = new Note();
        note.setUser(userRepository.getReferenceById(userId));
        apply(userId, note, request);
        noteRepository.save(note);
        return NoteMapper.toResponse(note);
    }

    @Transactional
    public NoteResponse update(Long userId, Long id, NoteRequest request) {
        Note note = requireOwned(userId, id);
        apply(userId, note, request);
        return NoteMapper.toResponse(note);
    }

    @Transactional
    public NoteResponse setPinned(Long userId, Long id, boolean pinned) {
        Note note = requireOwned(userId, id);
        note.setPinned(pinned);
        return NoteMapper.toResponse(note);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        noteRepository.delete(requireOwned(userId, id));
    }

    private Note requireOwned(Long userId, Long id) {
        return noteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note", id));
    }

    private void apply(Long userId, Note note, NoteRequest request) {
        note.setTitle(request.title().trim());
        note.setContent(request.content());
        note.setSubject(subjectService.resolveOptional(userId, request.subjectId()));
        note.setTags(normaliseTags(request.tags()));
        if (request.pinned() != null) {
            note.setPinned(request.pinned());
        }
    }

    /** Trims, lower-cases, collapses inner whitespace and removes duplicates, keeping input order. */
    static Set<String> normaliseTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags == null) {
            return result;
        }
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String clean = tag.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (!clean.isEmpty() && result.size() < MAX_TAGS) {
                result.add(clean);
            }
        }
        return result;
    }
}

package org.alterbit.aisme.document.api

import org.alterbit.aisme.document.SubjectService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SubjectsController(
    private val subjectService: SubjectService,
) {
    @GetMapping("/subjects")
    fun subjects(): SubjectsResponseDto {
        val subjects = subjectService.subjects().map { subject -> subject.toDto() }
        return SubjectsResponseDto(
            defaultSubjectId = subjects.firstOrNull()?.id,
            subjects = subjects,
        )
    }
}

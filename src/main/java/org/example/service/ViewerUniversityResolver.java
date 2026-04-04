package org.example.service;

/**
 * Определение university_id текущего пользователя для сравнения расписаний и каталогов.
 */
public interface ViewerUniversityResolver {

    long requireUniversityIdForEmail(String email);
}

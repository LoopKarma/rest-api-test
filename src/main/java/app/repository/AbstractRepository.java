package app.repository;

public interface AbstractRepository<T> {
    <S extends T> S save(S entity);
}

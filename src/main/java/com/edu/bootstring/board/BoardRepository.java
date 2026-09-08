package com.edu.bootstring.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByBoardCode(String boardCode);

    List<Board> findAllByOrderBySortOrderAsc();
}

package com.edu.bootstring.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findAllByRefTypeAndRefIdOrderByIdAsc(String refType, Long refId);

    long countByRefTypeAndRefId(String refType, Long refId);
}

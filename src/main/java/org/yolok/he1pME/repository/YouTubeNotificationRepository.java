package org.yolok.he1pME.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.yolok.he1pME.entity.YouTubeNotification;

import java.util.List;

@EnableScan
public interface YouTubeNotificationRepository extends CrudRepository<YouTubeNotification, String> {

    @NonNull
    List<YouTubeNotification> findAll();
}

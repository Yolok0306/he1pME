package org.yolok.he1pME.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.yolok.he1pME.entity.TwitchNotification;

import java.util.List;

@EnableScan
public interface TwitchNotificationRepository extends CrudRepository<TwitchNotification, String> {

    @NonNull
    List<TwitchNotification> findAll();
}

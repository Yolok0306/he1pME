package org.yolok.he1pME.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.yolok.he1pME.entity.CallAction;

import java.util.Optional;

@EnableScan
public interface CallActionRepository extends CrudRepository<CallAction, String> {

    Optional<CallAction> findByActionAndGuildId(String action, String guildId);

    Iterable<CallAction> findByGuildId(String guildId);
}

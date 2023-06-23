package org.yolok.he1pME.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.yolok.he1pME.entity.MemberData;

import java.util.Optional;

@EnableScan
public interface MemberDataRepository extends CrudRepository<MemberData, String> {

    Optional<MemberData> findByNameAndGuildId(String name, String guildId);
}

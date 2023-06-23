package org.yolok.he1pME.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.yolok.he1pME.entity.BadWord;

@EnableScan
public interface BadWordRepository extends CrudRepository<BadWord, String> {

}

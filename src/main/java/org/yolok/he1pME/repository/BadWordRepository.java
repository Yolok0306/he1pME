package org.yolok.he1pME.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.yolok.he1pME.entity.BadWord;

import java.util.List;

@EnableScan
public interface BadWordRepository extends CrudRepository<BadWord, String> {

    @NonNull
    List<BadWord> findAll();
}

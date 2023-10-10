package org.yolok.he1pME.annotation;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface He1pME {

    String instruction();

    String description();

    boolean nsfw() default false;

    Option[] options() default {};

    String example();

    @interface Option {

        OptionType optionType() default OptionType.STRING;

        String name();

        String description();

        boolean required() default true;
    }
}
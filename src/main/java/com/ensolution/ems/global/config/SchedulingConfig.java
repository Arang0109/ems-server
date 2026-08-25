package com.ensolution.ems.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주기 작업 활성화. 현재는 SSE 구독 연결의 하트비트가 유일한 사용처다
 * (유휴 연결이 프록시에서 끊기는 것을 막고, 죽은 구독을 걸러낸다).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

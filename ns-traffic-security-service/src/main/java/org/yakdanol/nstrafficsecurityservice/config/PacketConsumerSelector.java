//package org.yakdanol.nstrafficsecurityservice.config;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.yakdanol.nstrafficsecurityservice.service.DataSource;
//import org.yakdanol.nstrafficsecurityservice.service.consumer.PacketConsumer;
//
///**
// * Выбирает активный PacketConsumer (Kafka или File) на основании свойства
// * traffic-security.data-source и публикует его как @Primary-бин.
// */
//@Configuration
//public class PacketConsumerSelector {
//    private final TrafficSecurityConfig trafficSecurityConfig;
//
//    public PacketConsumerSelector(TrafficSecurityConfig trafficSecurityConfig) {
//        this.trafficSecurityConfig = trafficSecurityConfig;
//    }
//
//    /**
//     * @param kafkaConsumer бин, объявленный как "securityKafkaPacketConsumer"
//     * @param fileConsumer  бин, объявленный как "securityFilePacketConsumer"
//     * @return выбранный в соответствии с конфигурацией PacketConsumer
//     */
//    @Bean
//    @Primary
//    public PacketConsumer packetConsumer(
//            @Qualifier("securityKafkaPacketConsumer") PacketConsumer kafkaConsumer,
//            @Qualifier("securityFilePacketConsumer")  PacketConsumer fileConsumer) {
//        DataSource source = trafficSecurityConfig.getGeneralConfig().getDataSource();
//        return switch (source) {
//            case KAFKA -> kafkaConsumer;
//            case FILE -> fileConsumer;
//            default -> throw new IllegalStateException("Unsupported data-source: " + source);
//        };
//    }
//}

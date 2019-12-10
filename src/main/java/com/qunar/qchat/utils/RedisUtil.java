package com.qunar.qchat.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.util.Hashing;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
public class RedisUtil {
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private volatile StringRedisTemplate redisTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public <T> void set(int table, String key, T value, long timeout, TimeUnit timeUnit) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        set(key, value, timeout, timeUnit);
    }

    public <T> void set(int table, String key, T value) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            String valueStr = obj2String(value);
            valueOperations.set(key, valueStr);
        } catch (Exception e) {
            logger.info("向redis中存数据失败，key:{},configinfo:{}", key, value, e);
        }
    }

    /**
     * 向redis存数据，利用Jackson对对象进行序列化后再存储
     *
     * @param key      key
     * @param value    configinfo
     * @param timeout  过期时间
     * @param timeUnit 过期时间的单位 {@link TimeUnit}
     * @param <T>      value的类型
     */
    public <T> void set(String key, T value, long timeout, TimeUnit timeUnit) {
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            //   byte[] valueStr = redisTemplate.getValueSerializer().serialize(configinfo);
            //    String valueStr = JacksonUtil.obj2String(configinfo);
            String valueStr = obj2String(value);
            valueOperations.set(key, valueStr, timeout, timeUnit);
        } catch (Exception e) {
            logger.info("向redis中存数据失败，key:{},configinfo:{}", key, value, e);
        }

    }

    /**
     * 从redis获取数据，利用Jackson对json进行反序列化
     *
     * @param key   key
     * @param clazz 需要反序列化成的对象的class对象
     * @param <T>   class对象保留的对象类型
     * @return 取出来并反序列后的对象
     */

    public <T> T get(int table, String key, Class<T> clazz) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        return get(key, clazz);
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            String valueStr = valueOperations.get(key);
            return string2Obj(valueStr, clazz);
        } catch (Exception e) {
            logger.info("从redis中取数据失败，key:{}", key, e);
            return null;
        }
    }


    public <T> T get(int table, String key, TypeReference<T> typeReference) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        return get(key, typeReference);
    }

    /**
     * 从redis获取数据，利用Jackson对json进行反序列化
     *
     * @param key           key
     * @param typeReference 需要反序列成的对象，针对有泛型的对象
     * @param <T>           保留的对象类型
     * @return 取出来并反序列化后的对象
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            return string2Obj(valueOperations.get(key), typeReference);
        } catch (Exception e) {
            logger.info("从redis中取数据失败，key:{}", key, e);
            return null;
        }
    }

    public void remove(int table, String key) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        remove(key);
    }


    /**
     * 把redis的某个key-value对进行删除
     *
     * @param key key
     */
    public void remove(String key) {
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valueOperations.getOperations().delete(key);
        } catch (Exception e) {
            logger.info("从redis中删除数据失败，key:{}", key, e);
        }
    }

    /**
     * @param redisKey   key
     * @param increValue increValue
     * @param <T>        类型
     */
    @SuppressWarnings("unchecked")
    public <T> void incr(final T redisKey, final long increValue) {
        redisTemplate.execute(new RedisCallback<T>() {
            @Override
            public T doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer redisSerializer = redisTemplate.getValueSerializer();
                byte[] key = redisSerializer.serialize(redisKey);
                connection.incrBy(key, increValue);
                return null;
            }
        });

    }

    public Set<String> keys(int table){
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        try {
            return redisTemplate.keys("*");
        } catch (Exception e) {
            logger.info("向redis中存数据失败，key:{},e:{}", "", e);
        }

        return Sets.newHashSet();
    }
    public  Set<String> hGetKeys(int table,String key) {
        try {
            ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            return hashOperations.keys(key);
        } catch (Exception e) {
            logger.info("从redis中获取hash数据失败，key:{},hKeys:{}", key);
            return null;
        }
    }
    public Set<String> hkeys(int table, String key) {


        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        try {

            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            return hashOperations.keys(key);
        } catch (Exception e) {
            logger.info("向redis中存数据失败，key:{},e:{}", key, e);
        }
        return Sets.newHashSet();
    }

    public String hGet(int table, String key, String hashKey) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        return hGet(key, hashKey);
    }

    public String hGet(String key, String hashKey) {
        try {
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            return hashOperations.get(key, hashKey);
        } catch (Exception e) {
            logger.info("从redis中获取hash数据失败，key:{},hashKey:{}", key, hashKey, e);
            return null;
        }
    }

    public <T> void hPut(int table, String key, String hashKey, T hashValue) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        hPut(key, hashKey, hashValue);
    }

    public <T> void hPut(int table, String key, String hashKey, T hashValue,long timeout,TimeUnit timeUnit) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        hPut(key, hashKey, hashValue);
        redisTemplate.expire(key,timeout,timeUnit);
    }

    public <T> void hPut(String key, String hashKey, T hashValue) {
        try {
            HashOperations<String, String, T> hashOperations = redisTemplate.opsForHash();
            hashOperations.put(key, hashKey, hashValue);
        } catch (Exception e) {
            logger.info("向redis中插入hash数据失败，key:{},hashKey:{},hashValue:{}", key, hashKey, hashValue, e);
        }
    }

    public <T> void hDel(int table, String key, String hashKey) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        hDel(key, hashKey);
    }

    public <T> void hDel(String key, String hashKey) {
        try {
            HashOperations<String, String, T> hashOperations = redisTemplate.opsForHash();
            hashOperations.delete(key, hashKey);
        } catch (Exception e) {
            logger.info("向redis中删除hash数据失败，key:{},hashKey:{},e:{}", key, hashKey, e);
        }

    }

    /**
     * redis的key生成器
     *
     * @param prefix  key的前缀
     * @param objects 后面加的一些唯一标示
     * @return key
     */
    public String keyGenerator(String prefix, Object... objects) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object obj : objects) {
            sb.append("_").append(obj.toString());
        }
        return Long.toString(Hashing.MURMUR_HASH.hash(sb.toString()));
    }

    public <T> String obj2String(T obj) {

        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.info("serialize Object to String failed,Object:{}", obj.getClass(), e);
            return null;
        }
    }

    public <T> T string2Obj(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return String.class.equals(clazz) ? (T) json : objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.info(" deserialize String to Object failed,json:{},class:{}", json, clazz.getName(), e);
            return null;
        }
    }

    public <T> T string2Obj(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json) || typeReference == null) {
            return null;
        }
        try {
            return (T) (String.class.equals(typeReference.getType()) ? json : objectMapper.readValue(json, typeReference));
        } catch (IOException e) {
            logger.info(" deserialize String to Object failed,json:{},type:{}", json, typeReference.getType(), e);
            return null;
        }
    }


    public Map<String,String> hGetAll(int table, String key) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        return hGetAll(key);
    }

    public Map<String,String> hGetAll(String key) {
        try {
            HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
            return hashOperations.entries(key);

        } catch (Exception e) {
            logger.error("从redis中获取hGetAll数据失败，key:{},key:{}", key, e);
            return null;
        }
    }

    public Boolean expire(int table, String key, long timeout, TimeUnit timeUnit) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        return redisTemplate.expire(key, timeout, timeUnit);

    }

    public void delete(int table, String key) {
        ((JedisConnectionFactory) redisTemplate.getConnectionFactory()).setDatabase(table);
        redisTemplate.delete(key);

    }

    /**
     * retemplate相关配置
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 配置连接工厂
        template.setConnectionFactory(factory);

        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
        Jackson2JsonRedisSerializer jacksonSeial = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String,Integer等会跑出异常
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jacksonSeial.setObjectMapper(om);

        // 值采用json序列化
        template.setValueSerializer(jacksonSeial);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());

        // 设置hash key 和value序列化模式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jacksonSeial);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 对hash类型的数据操作
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public HashOperations<String, String, Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * 对redis字符串类型数据操作
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }

    /**
     * 对链表类型的数据操作
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public ListOperations<String, Object> listOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForList();
    }

    /**
     * 对无序集合类型的数据操作
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForSet();
    }

    /**
     * 对有序集合类型的数据操作
     *
     * @param redisTemplate
     * @return
     */
    @Bean
    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForZSet();
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jpbcdemo;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
/**
 *
 * @author lihongzhi
 */
public class RBACSC {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; // 使用CBC模式和PKCS5填充
    private static final int KEY_SIZE = 256; // AES-256
    private static final int IV_SIZE = 16; // 初始化向量大小(16字节)
    
    public static void main(String[] args) throws Exception{
        //SHA256Test();
        
        //AESTest();
        
        PointMultiTest();
    }
    
    public static void SHA256Test(){
          // 测试数据
        String input = "Hello, this is a test string for SHA-256 hashing.Hello, this is a test string for SHA-256 hashing.Hello, this is a test string for SHA-256 hashing.Hello, this is a test string for SHA-256 hashing.Hello, this is a test string for SHA-256 hashing.Hello, this is a test string for SHA-256 hashing.";
        
        // 单次执行测试
        System.out.println("=== Single execution test ===");
        long startTime = System.currentTimeMillis();
        String hash = calculateSHA256(input);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Input String: " + input);
        System.out.println("SHA-256 Hash: " + hash);
        System.out.println("Execute Time: " + (endTime - startTime) + " ms");
        
        // 多次执行测试（更准确的时间测量）
        System.out.println("\n=== Average Cost for Multipe Time===");
        int iterations = 1000;
        long totalTime = 0;
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            String tempInput = input + i; // 每次使用不同的输入避免JVM优化
            
            calculateSHA256(tempInput);
           
        }
        long end = System.currentTimeMillis();
        totalTime += (end - start);
        double averageTime = (double) totalTime / iterations;
        System.out.println("Times for test: " + iterations);
        System.out.printf("Average Time: %.5f ms%n", averageTime);
        System.out.printf("Hash Operations for a second: %,.0f times%n", 1000 / averageTime);
        
    }
    
    /**
     * 计算字符串的SHA-256哈希值
     * @param input 输入字符串
     * @return 64个字符的十六进制哈希字符串
     */
    public static String calculateSHA256(String input) {
        try {
            // 获取SHA-256消息摘要实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // 计算哈希值（UTF-8编码）
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256应该所有Java实现都支持，这里理论上不会执行
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
    
    public static void AESTest() throws Exception{
               // 1. 生成AES密钥
        long keyGenStart = System.currentTimeMillis();
        SecretKey secretKey = generateKey();
        long keyGenEnd = System.currentTimeMillis();
        System.out.printf("key generate time: %d ms%n", (keyGenEnd - keyGenStart));
        
        // 2. 生成初始化向量(IV)
        byte[] iv = generateIv();
        // 原始数据
        String originalText = "这是一段需要加密的敏感数据，包含中文和English混合内容！@#$%^&*()1234567890";
        System.out.println("\n原始文本: " + originalText);
        
        // 3. 加密
        long encryptStart = System.currentTimeMillis();
        String encryptedText = encrypt(originalText, secretKey, iv);
        long encryptEnd = System.currentTimeMillis();
        System.out.println("\nEncrypt Result(Base64): " + encryptedText);
        System.out.printf("Time for Encrypting: %d ms%n", (encryptEnd - encryptStart));
        
        // 4. 解密
        long decryptStart = System.currentTimeMillis();
        String decryptedText = decrypt(encryptedText, secretKey, iv);
        long decryptEnd = System.currentTimeMillis();
        System.out.println("\nDecrypt Result: " + decryptedText);
        System.out.printf("Time for Decryption: %d ms%n", (decryptEnd - decryptStart));
        
        // 5. 验证结果
        System.out.println("\nVerification: " + originalText.equals(decryptedText));
        
        // 6. 性能测试（多次执行）
        System.out.println("\n=== Performance Test（100 Times）===");
        int iterations = 100;
        long totalEncryptTime = 0;
        long totalDecryptTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            String tempText = originalText + i; // 每次使用不同输入
            
            long start = System.currentTimeMillis();
            String enc = encrypt(tempText, secretKey, iv);
            totalEncryptTime += System.currentTimeMillis() - start;
            
            start = System.currentTimeMillis();
            String dec = decrypt(enc, secretKey, iv);
            totalDecryptTime += System.currentTimeMillis() - start;
            
            if (!tempText.equals(dec)) {
                throw new RuntimeException("加解密验证失败！");
            }
        }
        
        System.out.printf("Average Encrypt Time: %.2f ms%n", (double)totalEncryptTime / iterations);
        System.out.printf("Average Decrypt Time: %.2f ms%n", (double)totalDecryptTime / iterations);
        System.out.printf("加密吞吐量: %.2f 次/秒%n", 1000 / ((double)totalEncryptTime / iterations));
        System.out.printf("解密吞吐量: %.2f 次/秒%n", 1000 / ((double)totalDecryptTime / iterations));
          
    }
    
        /**
     * 生成AES密钥
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, new SecureRandom());
        return keyGenerator.generateKey();
    }
    
    /**
     * 生成初始化向量(IV)
     */
    public static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * AES加密
     * @param input 原始文本
     * @param key 密钥
     * @param iv 初始化向量
     * @return Base64编码的加密结果
     */
    public static String encrypt(String input, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        
        byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * AES解密
     * @param encryptedText Base64编码的加密文本
     * @param key 密钥
     * @param iv 初始化向量
     * @return 解密后的原始文本
     */
    public static String decrypt(String encryptedText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    
    
    public static void PointMultiTest(){
        Pairing bp = PairingFactory.getPairing("D:\\实验代码\\jpbc-2.0.0\\params\\curves\\a.properties");
        Field Zr = bp.getZr();
        Field G1 = bp.getG1();
      
        Element x = Zr.newRandomElement();
        Element g = G1.newRandomElement();
        long start = System.currentTimeMillis();
        Element y = g.duplicate().powZn(x);
        long end = System.currentTimeMillis();
        System.out.println("time cost: " + (end-start));
    }
    
    
}

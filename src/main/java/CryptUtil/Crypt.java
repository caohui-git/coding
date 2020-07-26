package CryptUtil;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Crypt {
    public static String ta = "0123456789abcdef";
    public static String base64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final Map<Character, Byte> MAP = new HashMap<>();
    //字符-字节
    static {
        for (int i = 0; i < HEX_CHAR_TABLE.length; i++) {
            char c = HEX_CHAR_TABLE[i];
            MAP.put(c, (byte) i);
        }
    }
    //16进制字符串转化为byte数组
    public static byte[] toByteArray(String hexString) {
        byte[] result = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length() / 2; i++) {
            char hi = hexString.charAt(i * 2);
            char lo = hexString.charAt(i * 2 + 1);
            result[i] = (byte) ((MAP.get(hi) << 4) + MAP.get(lo));
        }
        return result;
    }
    //字符串转整数
    public int stringtonumber(String s){
       int num = 0;
       if(s == null)return 0;
       for(int i = 0; i < s.length(); ++i){
           num = 256 * num + s.charAt(i);
       }
       return num;
    }
    //字符串转化为16进制字符串
    public String stringtohex(String s){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); ++i){
            //sb.append("\\x");
            sb.append(Integer.toHexString((s.charAt(i))));
        }
        return sb.toString();
    }
    //字节数组变换位置
    public byte[] getByte(byte[] bytes, int type){
        int[][] table = {{11, 4, 10, 5, 3, 9, 15, 2, 8, 14, 1, 7, 13, 0, 6, 12},{63, 62, 20, 41, 40, 61, 19, 18, 39, 60, 59, 17, 38, 37, 58, 16, 15, 36, 57, 56, 14, 35, 34, 55, 13, 12, 33, 54, 53, 11, 32, 31, 52, 10, 9, 30, 51, 50, 8, 29, 28, 49, 7, 6, 27, 48, 47, 5, 26, 25, 46, 4, 3, 24, 45, 44, 2, 23, 22, 43, 1, 0, 21, 42}};
        int n = 16;
        if(type == 1) n = 64;
        byte[] tmp = new byte[n];
        for(int i = 0; i < n; ++i){
            tmp[i] = bytes[table[type][i]];
        }
        return tmp;
    }
    //base64编码
    public String base64EncodeBytes(byte[] bytes, int type){
        byte[] newBytes = getByte(bytes, type);
        int x = 22, up_bound = 128;
        if(type == 1) {
            x = 86;
            up_bound = 512;
        }
        StringBuilder sb = new StringBuilder();
        for(byte by : newBytes){
            sb.append(Integer.toBinaryString((by & 0xff) + 0x100).substring(1));
        }
        String binStr = sb.toString();
        StringBuilder sb2 = new StringBuilder();
        for(int i = 1; i < x; ++i){
            sb2.append(base64.charAt(Integer.parseInt(binStr.substring(up_bound - i * 6, up_bound - (i-1) * 6), 2)));
        }
        sb2.append(base64.charAt(Integer.parseInt(binStr.substring(0, 2), 2)));
        return sb2.toString();
    }
    //md5 crypt
    public String md5_hash(String password, String salt){
        if(salt.length() > 8)
            salt = salt.substring(0 ,8);
        String psp = password + salt + password;
        String hash_1 = DigestUtils.md5Hex(psp);
        StringBuilder sb = new StringBuilder();
        sb.append(stringtohex(password + "$1$" + salt));
        for(int i = 0; i < password.length() / 16; i++){
            sb.append(hash_1);
        }
        for(int j = 0; j < password.length() % 16; j++){
            sb.append(hash_1.substring(j * 2, (j + 1) * 2));
        }
        for(int k = password.length(); k != 0; k >>= 1){
            if ((k & 1) == 1){
                sb.append("00");
            }else{
                sb.append(ta.charAt(password.charAt(0) / 16));
                sb.append(ta.charAt(password.charAt(0) % 16));
            }
        }
        String tmp = DigestUtils.md5Hex(toByteArray(sb.toString()));
        for(int i = 0; i < 1000; ++i){
            StringBuilder sb_tmp =new StringBuilder();
            if((i & 1) == 1) sb_tmp.append(stringtohex(password));else sb_tmp.append(tmp);
            if(i % 3 != 0) sb_tmp.append(stringtohex(salt));
            if(i % 7 != 0) sb_tmp.append(stringtohex(password));
            if((i & 1) == 1) sb_tmp.append(tmp);else sb_tmp.append(stringtohex(password));
            tmp = DigestUtils.md5Hex(toByteArray(sb_tmp.toString()));
        }
        return base64EncodeBytes(toByteArray(tmp),0);
    }
    //sha512 crypt
    public String sha512_hash(String password, String salt, int rounds){
        if(salt.length() > 16)
            salt = salt.substring(0, 16);
        String alternate = DigestUtils.sha512Hex(password + salt +password);
        StringBuilder sb = new StringBuilder();
        sb.append(stringtohex(password + salt));
        for(int i = 0; i < password.length() / 16; i++){
            sb.append(alternate);
        }
        for(int j = 0; j < password.length() % 16; j++){
            sb.append(alternate.substring(j * 2, (j + 1) * 2));
        }

        for(int l = password.length(); l != 0; l >>= 1){
            if ((l & 1) == 1){
                sb.append(alternate);
            }else{
                sb.append(stringtohex(password));
            }
        }
        String inter = DigestUtils.sha512Hex(toByteArray(sb.toString()));
        int fistByte = MAP.get(inter.charAt(0)) * 16 + MAP.get(inter.charAt(1));
        StringBuilder sb_tmp = new StringBuilder();
        for(int i= 0; i < password.length(); ++i)
            sb_tmp.append(password);
        String p_bytes = DigestUtils.sha512Hex(sb_tmp.toString()).substring(0, password.length() * 2);
        StringBuilder sb_2 = new StringBuilder();
        for(int j = 0; j < (16 + fistByte); ++j)
            sb_2.append(salt);
        String s_bytes = DigestUtils.sha512Hex(sb_2.toString()).substring(0, salt.length() * 2);
        for(int s = 0; s < rounds; ++s){
            StringBuilder sb_3 =new StringBuilder();
            if((s & 1) == 1) sb_3.append(p_bytes);else sb_3.append(inter);
            if(s % 3 != 0) sb_3.append(s_bytes);
            if(s % 7 != 0) sb_3.append(p_bytes);
            if((s & 1) == 1) sb_3.append(inter);else sb_3.append(p_bytes);
            inter = DigestUtils.sha512Hex(toByteArray(sb_3.toString()));
        }
        return base64EncodeBytes(toByteArray(inter), 1);
    }
    public static void main(String[] args) throws NoSuchAlgorithmException {
        Crypt cr = new Crypt();
        //System.out.println(m.getMD5("123456", "2HZ86S8v"));
        //long a=0;
        //System.out.println(Integer.parseInt("11111010",2));
        //String s = "de20acad1078ff225b969b6b18f79f91";
        //byte[] ba = toByteArray(s);
        //System.out.println(m.base64EncodeBytes(ba));
        //byte[] byt = "123456".getBytes(UTF_8);
        //String tmp="70c2e18fc533c5a581cbe14ce40e38f6";
        System.out.println(cr.sha512_hash("123456", "asdfg", 5000));
        //String[] ss = "63, 62, 20, 41, 40, 61, 19, 18, 39, 60, 59, 17, 38, 37, 58, 16, 15, 36, 57, 56, 14, 35, 34, 55, 13, 12, 33, 54, 53, 11, 32, 31, 52, 10, 9, 30, 51, 50, 8, 29, 28, 49, 7, 6, 27, 48, 47, 5, 26, 25, 46, 4, 3, 24, 45, 44, 2, 23, 22, 43, 1, 0, 21, 42".split(",");
        //System.out.println(Integer.valueOf("6"));
    }
}

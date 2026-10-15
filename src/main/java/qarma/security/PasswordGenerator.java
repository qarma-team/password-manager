package qarma.security;
import java.security.SecureRandom;

public class PasswordGenerator {
    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*.";
    private static final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String nums = "0123456789";
    private static final String chars = "!@#$%^&*.";
    public String generate(int length){

        if (length < 8 || length > 128){
            throw new IllegalArgumentException("password length cannot be less than 8 or more than 128 characters");
        }

        StringBuilder sb = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        int numindex = secureRandom.nextInt(nums.length());
        int charindex = secureRandom.nextInt(chars.length());
        int letindex = secureRandom.nextInt(letters.length());
        int numpos;
        int charpos;
        int letpos;
        do{
            numpos = secureRandom.nextInt(length);
            charpos = secureRandom.nextInt(length);
            letpos = secureRandom.nextInt(length);
        }while(numpos == charpos || letpos == charpos || letpos == numpos);

        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(characters.length());
            if (i == numpos){
                sb.append(nums.charAt(numindex));
            } else if (i == charpos) {
                sb.append(chars.charAt(charindex));
            } else if(i == letpos){
                sb.append(letters.charAt(letindex));
            } else {
                sb.append(characters.charAt(index));
            }
        }

        return sb.toString();

    }
}



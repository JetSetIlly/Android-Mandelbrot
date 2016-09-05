package jetsetilly.tools;

public class MyMath {
    public static boolean isPrime(int n) {
        for (int i = 2; i < Math.sqrt(n); ++i) {
            if (n % i == 0) {
                // has divided so this is a composite number
                return false;
            }
        }
        return true;
    }

    public static boolean isEven(int n) {
        return n % 2 == 0;
    }
}

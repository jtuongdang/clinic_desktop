package vn.clinicmanagement.desktop.modules.print.reception;
import java.math.*;
import java.util.*;
/** Đọc số tiền VND không âm; số lẻ được thể hiện riêng để không làm mất dữ liệu. */
public final class SoTienBangChu {
    private SoTienBangChu() { }
    private static final String[] DIGITS = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    public static String read(BigDecimal amount) {
        if (amount == null) return "Chưa có đơn giá chốt cho lượt đăng ký";
        if (amount.signum() < 0) throw new IllegalArgumentException("Số tiền không được âm.");
        BigInteger integer = amount.toBigInteger();
        List<Integer> groups = new ArrayList<>();
        while (integer.signum() > 0) { BigInteger[] pair = integer.divideAndRemainder(BigInteger.valueOf(1000)); groups.add(pair[1].intValue()); integer = pair[0]; }
        StringBuilder result = new StringBuilder();
        for (int i = groups.size() - 1; i >= 0; i--) {
            if (groups.get(i) == 0) continue;
            if (result.length() > 0) result.append(' ');
            result.append(group(groups.get(i), i < groups.size() - 1));
            if (i % 3 == 1) result.append(" nghìn");
            if (i % 3 == 2) result.append(" triệu");
            result.append(" tỷ".repeat(i / 3));
        }
        if (result.length() == 0) result.append("không");
        result.append(" đồng");
        int cents = amount.remainder(BigDecimal.ONE).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
        if (cents > 0) result.append(" và ").append(group(cents, false)).append(" xu");
        result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        return result.toString();
    }
    private static String group(int value, boolean full) {
        StringBuilder text = new StringBuilder();
        int hundreds = value / 100, tens = value / 10 % 10, ones = value % 10;
        if (hundreds > 0 || full) text.append(DIGITS[hundreds]).append(" trăm");
        if (tens > 1) text.append(' ').append(DIGITS[tens]).append(" mươi");
        else if (tens == 1) text.append(" mười");
        else if (ones > 0 && text.length() > 0) text.append(" lẻ");
        if (ones > 0) text.append(' ').append(ones == 1 && tens > 1 ? "mốt" : ones == 5 && tens > 0 ? "lăm" : DIGITS[ones]);
        return text.toString().trim();
    }
}

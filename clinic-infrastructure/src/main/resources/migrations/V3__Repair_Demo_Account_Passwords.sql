-- Sửa hash placeholder đã được phát hành từ V2 trước khi có PBKDF2 verifier.
-- Chỉ cập nhật đúng hai tài khoản seed và đúng hash placeholder cũ.
UPDATE NhanVien
SET passwordHash = CASE username
    WHEN 'admin' THEN 'pbkdf2_sha256$210000$1CmHHyyKRFqB70+3bqIiAQ==$HccuNSaYa4j7W001EdKDppnLGOPBpdK99ClDkwnkfWA='
    WHEN 'tiennv' THEN 'pbkdf2_sha256$210000$pzwZ5lgrTRC3FUL54MMaXw==$msrLCG673s188PHhk9CWmmSzUIpoAJ2yH7Tz1CjotHA='
END
WHERE username IN ('admin', 'tiennv')
  AND passwordHash = '$2a$10$wWw4eM3lV8hV7p5P1r4PCOn9Q4fC8mH6FfE2wG8i7vC8o7o7o7o7.';

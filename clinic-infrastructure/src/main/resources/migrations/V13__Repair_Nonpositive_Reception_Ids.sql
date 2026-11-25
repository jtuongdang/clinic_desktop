-- Sửa các định danh không dương do dữ liệu cũ/reset identity.
-- Sao chép toàn bộ nội dung sang ID mới, chuyển FK rồi mới bỏ bản ghi mang ID lỗi.
-- Không sửa hay tắt kiểm tra của nhật ký kiểm toán bất biến.
DECLARE @table sysname, @object int, @old int, @new int, @seed bigint;
DECLARE @columns nvarchar(max), @sql nvarchar(max), @childSchema sysname, @childTable sysname, @childColumn sysname;
DECLARE reception_tables CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND name IN ('BaoHiem','LuotKham');
OPEN reception_tables;
FETCH NEXT FROM reception_tables INTO @table;
WHILE @@FETCH_STATUS=0
BEGIN
    SET @object=OBJECT_ID(QUOTENAME('dbo')+'.'+QUOTENAME(@table));
    SET @sql=N'SELECT @seed=CASE WHEN ISNULL(MAX(CONVERT(bigint,id)),0)>ISNULL(IDENT_CURRENT(N''dbo.'+@table+N'''),0) THEN ISNULL(MAX(CONVERT(bigint,id)),0) ELSE ISNULL(IDENT_CURRENT(N''dbo.'+@table+N'''),0) END FROM dbo.'+QUOTENAME(@table)+N' WITH (TABLOCKX,HOLDLOCK)';
    EXEC sp_executesql @sql,N'@seed bigint OUTPUT',@seed OUTPUT;
    IF @seed<1 SET @seed=1;
    SET @sql=N'DBCC CHECKIDENT (''dbo.'+@table+N''', RESEED, '+CONVERT(nvarchar(30),@seed)+N') WITH NO_INFOMSGS';
    EXEC sp_executesql @sql;
    SELECT @columns=STUFF((SELECT ','+QUOTENAME(name) FROM sys.columns WHERE object_id=@object
        AND is_identity=0 AND is_computed=0 AND generated_always_type=0 AND system_type_id<>189 ORDER BY column_id FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,'');
    WHILE 1=1
    BEGIN
        SET @old=NULL;
        SET @sql=N'SELECT TOP 1 @old=id FROM dbo.'+QUOTENAME(@table)+N' WHERE id<=0 ORDER BY id';
        EXEC sp_executesql @sql,N'@old int OUTPUT',@old OUTPUT;
        IF @old IS NULL BREAK;
        SET @new=NULL;
        SET @sql=N'DECLARE @ids TABLE(id int); INSERT INTO dbo.'+QUOTENAME(@table)+N' ('+@columns+N') OUTPUT INSERTED.id INTO @ids SELECT '+@columns+N' FROM dbo.'+QUOTENAME(@table)+N' WHERE id=@old; SELECT @new=id FROM @ids;';
        EXEC sp_executesql @sql,N'@old int,@new int OUTPUT',@old,@new OUTPUT;
        IF @new IS NULL OR @new<=0 THROW 51013,N'Không cấp được ID dương khi sửa dữ liệu tiếp đón.',1;

        DECLARE children CURSOR LOCAL FAST_FORWARD FOR
            SELECT OBJECT_SCHEMA_NAME(fc.parent_object_id),OBJECT_NAME(fc.parent_object_id),COL_NAME(fc.parent_object_id,fc.parent_column_id)
            FROM sys.foreign_key_columns fc WHERE fc.referenced_object_id=@object AND COL_NAME(fc.referenced_object_id,fc.referenced_column_id)='id';
        OPEN children;
        FETCH NEXT FROM children INTO @childSchema,@childTable,@childColumn;
        WHILE @@FETCH_STATUS=0
        BEGIN
            -- Chỉ UPDATE khi có liên kết thực tế, để không chạm trigger bất biến trên bảng rỗng.
            SET @sql=N'IF EXISTS (SELECT 1 FROM '+QUOTENAME(@childSchema)+'.'+QUOTENAME(@childTable)+N' WHERE '+QUOTENAME(@childColumn)+N'=@old) UPDATE '+QUOTENAME(@childSchema)+'.'+QUOTENAME(@childTable)+N' SET '+QUOTENAME(@childColumn)+N'=@new WHERE '+QUOTENAME(@childColumn)+N'=@old';
            EXEC sp_executesql @sql,N'@old int,@new int',@old,@new;
            FETCH NEXT FROM children INTO @childSchema,@childTable,@childColumn;
        END;
        CLOSE children; DEALLOCATE children;
        SET @sql=N'DELETE FROM dbo.'+QUOTENAME(@table)+N' WHERE id=@old';
        EXEC sp_executesql @sql,N'@old int',@old;
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=@object AND name='CK_'+@table+'_PositiveId')
    BEGIN
        SET @sql=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' WITH CHECK ADD CONSTRAINT '+QUOTENAME('CK_'+@table+'_PositiveId')+N' CHECK (id>0)';
        EXEC sp_executesql @sql;
    END;
    FETCH NEXT FROM reception_tables INTO @table;
END;
CLOSE reception_tables;
DEALLOCATE reception_tables;

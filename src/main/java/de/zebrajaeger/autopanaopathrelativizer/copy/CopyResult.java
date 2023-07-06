package de.zebrajaeger.autopanaopathrelativizer.copy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@ToString
public class CopyResult {
    private final List<FileCopyResult> resultList = new ArrayList<>();

    public boolean add(FileCopyResult fileCopyResult) {
        return resultList.add(fileCopyResult);
    }
    public boolean add(File source, File target, FileCopyResultType type) {
        return resultList.add(new FileCopyResult(source,target,type));
    }
    public boolean add(File source, File target, FileCopyResultType type, String msg) {
        return resultList.add(new FileCopyResult(source,target,type, msg));
    }
}

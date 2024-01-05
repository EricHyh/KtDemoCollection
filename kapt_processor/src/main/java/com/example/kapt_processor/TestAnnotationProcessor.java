package com.example.kapt_processor;

import com.example.kapt_annotation.TestAnnotation;
import com.google.auto.service.AutoService;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * TODO: Add Description
 *
 * @author eriche 2023/11/7
 */
@AutoService(Processor.class)
public class TestAnnotationProcessor extends AbstractProcessor
{


    private Filer filer;//文件生成
    private Messager messager;//日志
    private Elements elementUtil;//工具类

    private void log(String msg) {
        System.out.println(msg);
    }

    /**
     * 初始化操作
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        log("TestAnnotationProcessor init");
        filer = processingEnv.getFiler();
        elementUtil = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
    }


    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        log("TestAnnotationProcessor getSupportedAnnotationTypes");
        Set<String> set = new LinkedHashSet<>();
        set.add(TestAnnotation.class.getCanonicalName());
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        log("TestAnnotationProcessor getSupportedSourceVersion " + SourceVersion.latestSupported());
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        Set<? extends Element> rootElements = roundEnv.getRootElements();

        log("TestAnnotationProcessor process");


        return false;
    }
}

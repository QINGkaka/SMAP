package com.metriclab.service;

import com.metriclab.model.dto.ClassMetric;
import com.metriclab.model.dto.ComplexityAnalysisResult;
import com.metriclab.model.dto.EstimationResult;
import com.metriclab.model.dto.FunctionPointResult;
import com.metriclab.model.dto.LocAnalysisResult;
import com.metriclab.model.dto.LocFileMetric;
import com.metriclab.model.dto.MethodComplexityMetric;
import com.metriclab.model.dto.ObjectOrientedAnalysisResult;
import com.metriclab.model.dto.ModelAnalysisResult;
import com.metriclab.model.dto.ModelClassMetric;
import com.metriclab.model.dto.UseCasePointResult;
import com.metriclab.model.dto.XmlExportResult;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Service
public class XmlExportService {

    private final FileStorageService fileStorageService;
    private final LocAnalysisService locAnalysisService;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final ObjectOrientedAnalysisService objectOrientedAnalysisService;
    private final EstimationService estimationService;
    private final FunctionPointService functionPointService;
    private final UseCasePointService useCasePointService;
    private final ModelAnalysisService modelAnalysisService;

    public XmlExportService(
            FileStorageService fileStorageService,
            LocAnalysisService locAnalysisService,
            ComplexityAnalysisService complexityAnalysisService,
            ObjectOrientedAnalysisService objectOrientedAnalysisService,
            EstimationService estimationService,
            FunctionPointService functionPointService,
            UseCasePointService useCasePointService,
            ModelAnalysisService modelAnalysisService
    ) {
        this.fileStorageService = fileStorageService;
        this.locAnalysisService = locAnalysisService;
        this.complexityAnalysisService = complexityAnalysisService;
        this.objectOrientedAnalysisService = objectOrientedAnalysisService;
        this.estimationService = estimationService;
        this.functionPointService = functionPointService;
        this.useCasePointService = useCasePointService;
        this.modelAnalysisService = modelAnalysisService;
    }

    public synchronized XmlExportResult exportProjectMetrics(String projectId) throws IOException {
        LocAnalysisResult loc = locAnalysisService.latestResult(projectId);
        if (loc == null) {
            loc = locAnalysisService.analyzeProject(projectId);
        }

        ComplexityAnalysisResult complexity = complexityAnalysisService.latestResult(projectId);
        if (complexity == null) {
            complexity = complexityAnalysisService.analyzeProject(projectId);
        }

        ObjectOrientedAnalysisResult oo = objectOrientedAnalysisService.latestResult(projectId);
        if (oo == null) {
            oo = objectOrientedAnalysisService.analyzeProject(projectId);
        }

        EstimationResult estimation = estimationService.latestResult(projectId);
        if (estimation == null) {
            estimation = estimationService.analyzeProject(projectId, null);
        }

        FunctionPointResult functionPoint = functionPointService.latestResult(projectId);
        UseCasePointResult useCasePoint = useCasePointService.latestResult(projectId);
        ModelAnalysisResult modelAnalysis = modelAnalysisService.latestResult(projectId);

        String content = buildXml(projectId, loc, complexity, oo, estimation, functionPoint, useCasePoint, modelAnalysis);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("metrics.xml");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new XmlExportResult(projectId, reportPath.toString(), content, OffsetDateTime.now());
    }

    private String buildXml(
            String projectId,
            LocAnalysisResult loc,
            ComplexityAnalysisResult complexity,
            ObjectOrientedAnalysisResult oo,
            EstimationResult estimation,
            FunctionPointResult functionPoint,
            UseCasePointResult useCasePoint,
            ModelAnalysisResult modelAnalysis
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<metricProject id=\"").append(xml(projectId)).append("\">\n");
        builder.append("  <loc taskId=\"").append(xml(loc.taskId())).append("\" analyzedAt=\"").append(xml(loc.analyzedAt().toString())).append("\">\n");
        builder.append("    <summary fileCount=\"").append(loc.summary().fileCount())
                .append("\" totalLines=\"").append(loc.summary().totalLines())
                .append("\" sourceLines=\"").append(loc.summary().sourceLines())
                .append("\" commentLines=\"").append(loc.summary().commentLines())
                .append("\" blankLines=\"").append(loc.summary().blankLines())
                .append("\" commentRate=\"").append(loc.summary().commentRate())
                .append("\" />\n");
        builder.append("    <files>\n");
        for (LocFileMetric file : loc.files()) {
            builder.append("      <file name=\"").append(xml(file.fileName()))
                    .append("\" source=\"").append(xml(file.sourceUploadName()))
                    .append("\" totalLines=\"").append(file.totalLines())
                    .append("\" sourceLines=\"").append(file.sourceLines())
                    .append("\" commentLines=\"").append(file.commentLines())
                    .append("\" blankLines=\"").append(file.blankLines())
                    .append("\" commentRate=\"").append(file.commentRate())
                    .append("\" />\n");
        }
        builder.append("    </files>\n");
        builder.append("  </loc>\n");

        builder.append("  <complexity taskId=\"").append(xml(complexity.taskId())).append("\" analyzedAt=\"")
                .append(xml(complexity.analyzedAt().toString())).append("\">\n");
        builder.append("    <summary fileCount=\"").append(complexity.summary().fileCount())
                .append("\" methodCount=\"").append(complexity.summary().methodCount())
                .append("\" averageComplexity=\"").append(complexity.summary().averageComplexity())
                .append("\" maxComplexity=\"").append(complexity.summary().maxComplexity())
                .append("\" highRiskMethodCount=\"").append(complexity.summary().highRiskMethodCount())
                .append("\" />\n");
        builder.append("    <methods>\n");
        for (MethodComplexityMetric method : complexity.methods()) {
            builder.append("      <method name=\"").append(xml(method.methodName()))
                    .append("\" file=\"").append(xml(method.fileName()))
                    .append("\" source=\"").append(xml(method.sourceUploadName()))
                    .append("\" startLine=\"").append(method.startLine())
                    .append("\" endLine=\"").append(method.endLine())
                    .append("\" cyclomaticComplexity=\"").append(method.cyclomaticComplexity())
                    .append("\" riskLevel=\"").append(xml(method.riskLevel()))
                    .append("\" />\n");
        }
        builder.append("    </methods>\n");
        builder.append("  </complexity>\n");

        builder.append("  <objectOriented taskId=\"").append(xml(oo.taskId())).append("\" analyzedAt=\"")
                .append(xml(oo.analyzedAt().toString())).append("\">\n");
        builder.append("    <summary fileCount=\"").append(oo.summary().fileCount())
                .append("\" classCount=\"").append(oo.summary().classCount())
                .append("\" interfaceCount=\"").append(oo.summary().interfaceCount())
                .append("\" methodCount=\"").append(oo.summary().methodCount())
                .append("\" fieldCount=\"").append(oo.summary().fieldCount())
                .append("\" averageCbo=\"").append(oo.summary().averageCbo())
                .append("\" averageRfc=\"").append(oo.summary().averageRfc())
                .append("\" maxDit=\"").append(oo.summary().maxDit())
                .append("\" highRiskClassCount=\"").append(oo.summary().highRiskClassCount())
                .append("\" />\n");
        builder.append("    <classes>\n");
        for (ClassMetric item : oo.classes()) {
            builder.append("      <class name=\"").append(xml(item.className()))
                    .append("\" type=\"").append(xml(item.type()))
                    .append("\" file=\"").append(xml(item.fileName()))
                    .append("\" source=\"").append(xml(item.sourceUploadName()))
                    .append("\" cbo=\"").append(item.cbo())
                    .append("\" rfc=\"").append(item.rfc())
                    .append("\" dit=\"").append(item.dit())
                    .append("\" noc=\"").append(item.noc())
                    .append("\" noa=\"").append(item.noa())
                    .append("\" noo=\"").append(item.noo())
                    .append("\" cs=\"").append(item.cs())
                    .append("\" wmc=\"").append(item.wmc())
                    .append("\" lcom=\"").append(item.lcom())
                    .append("\" riskLevel=\"").append(xml(item.riskLevel()))
                    .append("\" />\n");
        }
        builder.append("    </classes>\n");
        builder.append("  </objectOriented>\n");

        builder.append("  <estimation taskId=\"").append(xml(estimation.taskId())).append("\" analyzedAt=\"")
                .append(xml(estimation.analyzedAt().toString())).append("\">\n");
        builder.append("    <summary mode=\"").append(xml(estimation.mode()))
                .append("\" modeLabel=\"").append(xml(estimation.modeLabel()))
                .append("\" kloc=\"").append(estimation.kloc())
                .append("\" scaleSource=\"").append(xml(estimation.scaleSource()))
                .append("\" effortPersonMonths=\"").append(estimation.effortPersonMonths())
                .append("\" developmentMonths=\"").append(estimation.developmentMonths())
                .append("\" averageStaff=\"").append(estimation.averageStaff())
                .append("\" costPerPersonMonth=\"").append(estimation.costPerPersonMonth())
                .append("\" estimatedCost=\"").append(estimation.estimatedCost())
                .append("\" />\n");
        builder.append("  </estimation>\n");
        if (functionPoint != null) {
            builder.append("  <functionPoint taskId=\"").append(xml(functionPoint.taskId())).append("\" analyzedAt=\"")
                    .append(xml(functionPoint.analyzedAt().toString())).append("\">\n");
            builder.append("    <summary countMode=\"").append(xml(functionPoint.countMode()))
                    .append("\" externalInputs=\"").append(functionPoint.externalInputs())
                    .append("\" externalOutputs=\"").append(functionPoint.externalOutputs())
                    .append("\" externalInquiries=\"").append(functionPoint.externalInquiries())
                    .append("\" internalLogicalFiles=\"").append(functionPoint.internalLogicalFiles())
                    .append("\" externalInterfaceFiles=\"").append(functionPoint.externalInterfaceFiles())
                    .append("\" unadjustedFunctionPoints=\"").append(functionPoint.unadjustedFunctionPoints())
                    .append("\" generalSystemCharacteristicTotal=\"").append(functionPoint.generalSystemCharacteristicTotal())
                    .append("\" valueAdjustmentFactor=\"").append(functionPoint.valueAdjustmentFactor())
                    .append("\" adjustedFunctionPoints=\"").append(functionPoint.adjustedFunctionPoints())
                    .append("\" />\n");
            if (functionPoint.componentSummaries() != null && !functionPoint.componentSummaries().isEmpty()) {
                builder.append("    <components>\n");
                functionPoint.componentSummaries().forEach(item -> builder.append("      <component code=\"")
                        .append(xml(item.code()))
                        .append("\" label=\"").append(xml(item.label()))
                        .append("\" itemCount=\"").append(item.itemCount())
                        .append("\" lowCount=\"").append(item.lowCount())
                        .append("\" averageCount=\"").append(item.averageCount())
                        .append("\" highCount=\"").append(item.highCount())
                        .append("\" functionPoints=\"").append(item.functionPoints())
                        .append("\" />\n"));
                builder.append("    </components>\n");
            }
            if (functionPoint.detailItems() != null && !functionPoint.detailItems().isEmpty()) {
                builder.append("    <detailItems>\n");
                functionPoint.detailItems().forEach(item -> builder.append("      <item code=\"")
                        .append(xml(item.code()))
                        .append("\" label=\"").append(xml(item.label()))
                        .append("\" name=\"").append(xml(item.name()))
                        .append("\" det=\"").append(item.det())
                        .append("\" ret=\"").append(item.ret() == null ? "" : item.ret())
                        .append("\" ftr=\"").append(item.ftr() == null ? "" : item.ftr())
                        .append("\" complexity=\"").append(xml(item.complexity()))
                        .append("\" functionPoints=\"").append(item.functionPoints())
                        .append("\" />\n"));
                builder.append("    </detailItems>\n");
            }
            builder.append("  </functionPoint>\n");
        }
        if (useCasePoint != null) {
            builder.append("  <useCasePoint taskId=\"").append(xml(useCasePoint.taskId())).append("\" analyzedAt=\"")
                    .append(xml(useCasePoint.analyzedAt().toString())).append("\">\n");
            builder.append("    <summary actorWeight=\"").append(useCasePoint.actorWeight())
                    .append("\" useCaseWeight=\"").append(useCasePoint.useCaseWeight())
                    .append("\" unadjustedUseCasePoints=\"").append(useCasePoint.unadjustedUseCasePoints())
                    .append("\" technicalFactorTotal=\"").append(useCasePoint.technicalFactorTotal())
                    .append("\" technicalComplexityFactor=\"").append(useCasePoint.technicalComplexityFactor())
                    .append("\" environmentalFactorTotal=\"").append(useCasePoint.environmentalFactorTotal())
                    .append("\" environmentalComplexityFactor=\"").append(useCasePoint.environmentalComplexityFactor())
                    .append("\" useCasePoints=\"").append(useCasePoint.useCasePoints())
                    .append("\" estimatedHours=\"").append(useCasePoint.estimatedHours())
                    .append("\" estimatedPersonMonths=\"").append(useCasePoint.estimatedPersonMonths())
                    .append("\" />\n");
            builder.append("  </useCasePoint>\n");
        }
        if (modelAnalysis != null) {
            builder.append("  <modelAnalysis taskId=\"").append(xml(modelAnalysis.taskId())).append("\" analyzedAt=\"")
                    .append(xml(modelAnalysis.analyzedAt().toString())).append("\">\n");
            builder.append("    <summary fileCount=\"").append(modelAnalysis.summary().fileCount())
                    .append("\" classCount=\"").append(modelAnalysis.summary().classCount())
                    .append("\" interfaceCount=\"").append(modelAnalysis.summary().interfaceCount())
                    .append("\" attributeCount=\"").append(modelAnalysis.summary().attributeCount())
                    .append("\" operationCount=\"").append(modelAnalysis.summary().operationCount())
                    .append("\" inheritanceRelationCount=\"").append(modelAnalysis.summary().inheritanceRelationCount())
                    .append("\" highRiskClassCount=\"").append(modelAnalysis.summary().highRiskClassCount())
                    .append("\" />\n");
            builder.append("    <classes>\n");
            for (ModelClassMetric item : modelAnalysis.classes()) {
                builder.append("      <modelClass name=\"").append(xml(item.className()))
                        .append("\" type=\"").append(xml(item.type()))
                        .append("\" source=\"").append(xml(item.sourceUploadName()))
                        .append("\" attributeCount=\"").append(item.attributeCount())
                        .append("\" operationCount=\"").append(item.operationCount())
                        .append("\" childCount=\"").append(item.childCount())
                        .append("\" inheritanceDepth=\"").append(item.inheritanceDepth())
                        .append("\" classSize=\"").append(item.classSize())
                        .append("\" parentName=\"").append(xml(item.parentName()))
                        .append("\" riskLevel=\"").append(xml(item.riskLevel()))
                        .append("\" />\n");
            }
            builder.append("    </classes>\n");
            builder.append("  </modelAnalysis>\n");
        }
        builder.append("</metricProject>\n");
        return builder.toString();
    }

    private String xml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;");
    }
}

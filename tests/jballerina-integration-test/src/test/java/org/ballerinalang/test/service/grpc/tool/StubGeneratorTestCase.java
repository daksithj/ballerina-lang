/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.test.service.grpc.tool;

import org.ballerinalang.compiler.BLangCompilerException;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.packerina.cmd.CommandUtil;
import org.ballerinalang.protobuf.cmd.GrpcCmd;
import org.ballerinalang.protobuf.cmd.OSDetector;
import org.ballerinalang.protobuf.utils.BalFileGenerationUtils;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.CompileResult;
import org.ballerinalang.test.util.TestUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConstant;
import org.wso2.ballerinalang.compiler.tree.types.BLangFiniteTypeNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ballerinalang.net.grpc.proto.ServiceProtoConstants.TMP_DIRECTORY_PATH;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Protobuf to bal generation function testcase.
 */
public class StubGeneratorTestCase {

    private static String protoExeName = "protoc-" + OSDetector.getDetectedClassifier() + ".exe";
    private Path resourceDir;
    private Path outputDirPath;
    private Path proxyServiceDirPath;

    @BeforeClass
    private void setup() throws Exception {
        TestUtils.prepareBalo(this);
        resourceDir = Paths.get("src", "test", "resources", "grpc", "src", "tool").toAbsolutePath();
        outputDirPath = Paths.get(TMP_DIRECTORY_PATH, "grpc");
        BalFileGenerationUtils.delete(outputDirPath.toFile());
        Path projectDirPath = Paths.get(TMP_DIRECTORY_PATH, "grpc", "proxyservice");
        Files.createDirectories(projectDirPath);
        CommandUtil.initProject(projectDirPath);
        proxyServiceDirPath = Paths.get(TMP_DIRECTORY_PATH, "grpc", "proxyservice");
    }

    @Test
    public void testUnaryHelloWorld() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorld.proto", outputDirPath, "helloWorld_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 13,
                "Expected functions not found in compile results.");
        validatePublicAttachedFunctions(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test(description = "Test proto definition compilation when proto file inside a directory with white space")
    public void testDirectoryWithSpace() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        CompileResult compileResult = getStubCompileResult(Paths.get("a b", "helloWorld.proto").toString(),
                outputDirPath.resolve("a b"), "helloWorld_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 13,
                "Expected functions not found in compile results.");
        validatePublicAttachedFunctions(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test(description = "Test service stub generation for service definition with dependency")
    public void testUnaryHelloWorldWithDependency() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithDependency.proto",
                 outputDirPath, "helloWorldWithDependency_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 10);
        assertEquals(compileResult.getDiagnostics()[0].toString(),
                     "ERROR: .::helloWorldWithDependency_pb.bal:15:34:: unknown type 'HelloRequest'");
        assertEquals(compileResult.getDiagnostics()[1].toString(),
                     "ERROR: .::helloWorldWithDependency_pb.bal:15:90:: unknown type 'HelloResponse'");
        assertEquals(compileResult.getDiagnostics()[5].toString(),
                     "ERROR: .::helloWorldWithDependency_pb.bal:26:86:: unknown type 'ByeResponse'");
    }

    @Test(description = "Test service stub generation for service definition with enum messages")
    public void testUnaryHelloWorldWithEnum() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithEnum.proto", outputDirPath,
                "helloWorldWithEnum_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 11,
                "Expected type definitions not found in compile results.");
        validateEnumNode(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 4,
                "Expected constants not found in compile results.");
        validateConstantNode(compileResult, "SENTIMENT_HAPPY");
        validateConstantNode(compileResult, "SENTIMENT_SAD");
        validateConstantNode(compileResult, "SENTIMENT_NEUTRAL");
    }

    @Test(description = "Test service stub generation for service definition with nested enum messages")
    public void testUnaryHelloWorldWithNestedEnum() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithNestedEnum.proto",
                outputDirPath, "helloWorldWithNestedEnum_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 11,
                "Expected type definitions not found in compile results.");
        validateEnumNode(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 4,
                "Expected constants not found in compile results.");
        validateConstantNode(compileResult, "SENTIMENT_HAPPY");
        validateConstantNode(compileResult, "SENTIMENT_SAD");
        validateConstantNode(compileResult, "SENTIMENT_NEUTRAL");
    }

    @Test(description = "Test service stub generation for service definition with nested messages")
    public void testUnaryHelloWorldWithNestedMessage() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithNestedMessage.proto",
                outputDirPath, "helloWorldWithNestedMessage_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 9,
                "Expected type definitions not found in compile results.");
    }

    @Test(description = "Test stub generation for with nested maps with same name")
    public void testUnaryHelloWorldWithMaps() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithMap.proto",
                outputDirPath, "helloWorldWithMap_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
    }

    @Test(description = "Test service stub generation for service definition with reserved names")
    public void testUnaryHelloWorldWithReservedNames() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithReservedNames.proto",
                outputDirPath, "helloWorldWithReservedNames_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
    }

    @Test(description = "Test service stub generation for service definition with invalid dependency",
            expectedExceptions = BLangCompilerException.class,
            expectedExceptionsMessageRegExp = "cannot find file 'helloWorldWithInvalidDependency_pb.bal'")
    public void testUnaryHelloWorldWithInvalidDependency() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithInvalidDependency.proto",
                outputDirPath, "helloWorldWithInvalidDependency_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 1);
    }

    @Test(description = "Test service stub generation tool for package service")
    public void testUnaryHelloWorldWithPackage() throws IllegalAccessException,
            ClassNotFoundException, InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldWithPackage.proto",
                outputDirPath, "helloWorldWithPackage_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 13,
                "Expected functions not found in compile results.");
        validatePublicAttachedFunctions(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test(description = "Test service stub generation tool command without specifying output directory path")
    public void testUnaryHelloWorldWithoutOutputPath() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCmd1 = (GrpcCmd) grpcCmd.newInstance();
        Path protoFilePath = resourceDir.resolve("helloWorld.proto");
        grpcCmd1.setProtoPath(protoFilePath.toAbsolutePath().toString());
        try {
            grpcCmd1.execute();
            Path sourceFileRoot = Paths.get("temp", "helloWorld_pb.bal");
            CompileResult compileResult = BCompileUtil.compile(sourceFileRoot.toAbsolutePath().toString());
            assertEquals(compileResult.getDiagnostics().length, 0);
            assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                    "Expected type definitions not found in compile results.");
            assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 13,
                    "Expected functions not found in compile results.");
            validatePublicAttachedFunctions(compileResult);
            assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                    "Expected global variables not found in compile results.");
            assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                    "Expected constants not found in compile results.");
            assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                    "Expected imports not found in compile results.");
        } finally {
            if (Paths.get("temp", "helloWorld_pb.bal").toFile().exists()) {
                BalFileGenerationUtils.delete(Paths.get("temp").toFile());
            }
        }
    }

    @Test
    public void testClientStreamingHelloWorld() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldClientStreaming.proto",
                outputDirPath, "helloWorldClientStreaming_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 4,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 6,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test
    public void testServerStreamingHelloWorld() throws IllegalAccessException,
            ClassNotFoundException, InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldServerStreaming.proto",
                outputDirPath, "helloWorldServerStreaming_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 4,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 6,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test
    public void testStandardDataTypes() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        CompileResult compileResult = getStubCompileResult("helloWorldString.proto",
                outputDirPath, "helloWorldString_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 3,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 7,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test
    public void testDifferentOutputPath() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCmd1 = (GrpcCmd) grpcCmd.newInstance();
        Path tempDirPath = outputDirPath.resolve("client");
        Path protoPath = Paths.get("helloWorld.proto");
        Path protoRoot = resourceDir.resolve(protoPath);
        grpcCmd1.setBalOutPath(tempDirPath.toAbsolutePath().toString());
        grpcCmd1.setProtoPath(protoRoot.toAbsolutePath().toString());
        grpcCmd1.execute();
        Path sourceFileRoot = Paths.get(TMP_DIRECTORY_PATH, "grpc", "client", "helloWorld_pb.bal");
        CompileResult compileResult = BCompileUtil.compile(sourceFileRoot.toString());
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 13,
                "Expected functions not found in compile results.");
        validatePublicAttachedFunctions(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test
    public void testServiceWithDescriptorMap() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCommand = (GrpcCmd) grpcCmd.newInstance();
        Path tempDirPath = outputDirPath.resolve("service");
        Path protoPath = Paths.get("helloWorld.proto");
        Path protoRoot = resourceDir.resolve(protoPath);
        grpcCommand.setBalOutPath(tempDirPath.toAbsolutePath().toString());
        grpcCommand.setProtoPath(protoRoot.toAbsolutePath().toString());
        grpcCommand.setMode("service");
        grpcCommand.execute();
        Path sampleServiceFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "service", "helloWorld_sample_service.bal");
        assertTrue(Files.exists(sampleServiceFile));
    }

    @Test
    public void testAutoGeneratedGrpcClient() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCommand = (GrpcCmd) grpcCmd.newInstance();
        Path tempDirPath = outputDirPath.resolve("client");
        Path protoPath = Paths.get("helloWorld.proto");
        Path protoRoot = resourceDir.resolve(protoPath);
        grpcCommand.setBalOutPath(tempDirPath.toAbsolutePath().toString());
        grpcCommand.setProtoPath(protoRoot.toAbsolutePath().toString());
        grpcCommand.setMode("client");
        grpcCommand.execute();
        Path sampleServiceFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "client", "helloWorld_sample_client.bal");
        assertTrue(Files.exists(sampleServiceFile));
    }

    @Test(description = "Test case for oneof field record generation")
    public void testOneofFieldRecordGeneration() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("oneof_field_service.proto",
                outputDirPath, "oneof_field_service_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 8,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 35,
                "Expected functions not found in compile results.");
        validatePublicAttachedFunctions(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test(description = "Test gateway proxy with only path and query parameters")
    public void testHelloWorldGateway() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGateway.proto",
                "helloWorldGateway");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 8,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 21,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 4,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy including mapping from the body")
    public void testHelloWorldGatewayWithBody() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGatewayWithBody.proto",
                "helloWorldGatewayWithBody");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 9,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 22,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 4,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy with primitive type input")
    public void testHelloWorldGatewayWithPrimitiveInput() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGatewayWithPrimitiveInput.proto",
                "helloWorldGatewayWithPrimitiveInput");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 4,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 17,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 4,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy with a repeated field in the input")
    public void testHelloWorldGatewayWithRepeatedField() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGatewayWithRepeatedField.proto",
                "helloWorldGatewayWithRepeatedField");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 9,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 22,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 4,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy with a nested message in the input")
    public void testHelloWorldGatewayWithNestedMessage() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGatewayWithNestedMessage.proto",
                "helloWorldGatewayWithNestedMessage");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 9,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 22,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 4,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy with an empty input")
    public void testHelloWorldGatewayWithEmptyInput() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGatewayWithEmptyInput.proto",
                "helloWorldGatewayWithEmptyInput");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 6,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 15,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 4,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 1);
    }

    @Test(description = "Test gateway proxy without a http method definition")
    public void testHelloWorldGatewayWithoutPath() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("helloWorldGatewayWithoutPath.proto",
                "helloWorldGatewayWithoutPath");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 1,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 6,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 10,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 0);
    }

    @Test(description = "Test gateway proxy for server streaming")
    public void testServerStreamingGateway() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("serverStreamingGateway.proto",
                "serverStreamingGateway");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 8,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 24,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 12,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy for client streaming")
    public void testClientStreamingGateway() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("clientStreamingGateway.proto",
                "clientStreamingGateway");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 8,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 24,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 12,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy for bi-directional streaming")
    public void testBidiStreamingGateway() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("bidiStreamingGateway.proto",
                "bidiStreamingGateway");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 8,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 24,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 12,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 2);
    }

    @Test(description = "Test gateway proxy for streaming client with a complete body input")
    public void testStreamingGatewayWithStarBody() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("streamingGatewayWithStarBody.proto",
                "streamingGatewayWithStarBody");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 10,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 31,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 14,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 3);
    }

    @Test(description = "Test gateway proxy for streaming with a primitive type input")
    public void testStreamingGatewayWithPrimitiveInput() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("streamingGatewayWithPrimitiveInput.proto",
                "streamingGatewayWithPrimitiveInput");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 7,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 28,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 14,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 3);
    }

    @Test(description = "Test gateway proxy for streaming with empty input")
    public void testStreamingGatewayWithEmptyInput() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("streamingGatewayWithEmptyInput.proto",
                "streamingGatewayWithEmptyInput");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 2,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 9,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 30,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 14,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 7,
                "Expected imports not found in compile results.");
        validateAttachedResources(compileResult, 3);
    }

    @Test(description = "Test gateway proxy for streaming without a path")
    public void testStreamingGatewayWithoutPath() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getProxyCompileResult("streamingGatewayWithoutPath.proto",
                "streamingGatewayWithoutPath");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).getCompilationUnits().size(), 1,
                "Expected compilation units not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 6,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 10,
                "Expected functions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test(description = "Test case for protobuf any type generation")
    public void testAnyTypeGeneration() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        CompileResult compileResult = getStubCompileResult("anydata.proto", outputDirPath,
                "anydata_pb.bal");
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).typeDefinitions.size(), 5,
                "Expected type definitions not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).functions.size(), 9,
                "Expected functions not found in compile results.");
        validatePublicAttachedFunctions(compileResult);
        assertEquals(((BLangPackage) compileResult.getAST()).globalVars.size(), 1,
                "Expected global variables not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants not found in compile results.");
        assertEquals(((BLangPackage) compileResult.getAST()).imports.size(), 1,
                "Expected imports not found in compile results.");
    }

    @Test(description = "Test case checks creation of only the service file, in the service mode, with single service")
    public void testServiceFileGenWithoutStub() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCommand = (GrpcCmd) grpcCmd.newInstance();
        Path tempDirPath = outputDirPath.resolve("service");
        Path protoPath = Paths.get("helloWorld.proto");
        Path protoRoot = resourceDir.resolve(protoPath);
        grpcCommand.setBalOutPath(tempDirPath.toAbsolutePath().toString());
        grpcCommand.setProtoPath(protoRoot.toAbsolutePath().toString());
        grpcCommand.setMode("service");
        grpcCommand.execute();
        Path sampleServiceFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "service", "helloWorld_sample_service.bal");

        // This file should not be created when --mode service enabled with one service
        Path sampleStubFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "service", "helloWorld_pb.bal");

        assertTrue(Files.exists(sampleServiceFile));
        assertFalse(Files.exists(sampleStubFile));

        CompileResult compileResult = BCompileUtil.compileOnly(sampleServiceFile.toString());
        assertEquals(compileResult.getDiagnostics().length, 0);
        assertEquals(((BLangPackage) compileResult.getAST()).constants.size(), 1,
                "Expected constants count not found." +
                        ((BLangPackage) compileResult.getAST()).constants.size()
        );
        assertEquals(((BLangPackage) compileResult.getAST()).services.size(), 1,
                "Expected services count not found. " +
                        ((BLangPackage) compileResult.getAST()).services.size()
        );
        assertEquals(((BLangPackage) compileResult.getAST()).getTypeDefinitions().size(), 6,
                "Expected type definitions count not found." +
                        ((BLangPackage) compileResult.getAST()).getTypeDefinitions().size()
        );
    }

    @Test(description = "Test case checks creation of only the service file," +
            " in the service mode, with multiple services")
    public void testServiceFilesGenForMultipleServices() throws IllegalAccessException, ClassNotFoundException,
            InstantiationException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCommand = (GrpcCmd) grpcCmd.newInstance();
        Path tempDirPath = outputDirPath.resolve("service");
        Path protoPath = Paths.get("multipleServices.proto");
        Path protoRoot = resourceDir.resolve(protoPath);
        grpcCommand.setBalOutPath(tempDirPath.toAbsolutePath().toString());
        grpcCommand.setProtoPath(protoRoot.toAbsolutePath().toString());
        grpcCommand.setMode("service");
        grpcCommand.execute();
        Path providerServiceFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "service", "Provider_sample_service.bal");
        Path registerServiceFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "service", "Register_sample_service.bal");
        Path stubFile = Paths.get(TMP_DIRECTORY_PATH, "grpc", "service", "multipleServices_pb.bal");

        assertTrue(Files.exists(providerServiceFile));
        assertTrue(Files.exists(registerServiceFile));
        assertTrue(Files.exists(stubFile));

        CompileResult providerCompileResult = BCompileUtil.compileOnly(providerServiceFile.toString());
        CompileResult registerCompileResult = BCompileUtil.compileOnly(registerServiceFile.toString());
        CompileResult stubCompileResult = BCompileUtil.compileOnly(stubFile.toString());

        assertEquals(((BLangPackage) providerCompileResult.getAST()).globalVars.size(), 1,
                "Expected constants count not found." +
                        ((BLangPackage) providerCompileResult.getAST()).globalVars.size()
        );
        assertEquals(((BLangPackage) providerCompileResult.getAST()).services.size(), 1,
                "Expected services count not found. " +
                        ((BLangPackage) providerCompileResult.getAST()).services.size()
        );
        assertEquals(((BLangPackage) providerCompileResult.getAST()).getTypeDefinitions().size(), 1,
                "Expected type definitions count not found." +
                        ((BLangPackage) providerCompileResult.getAST()).getTypeDefinitions().size()
        );

        assertEquals(((BLangPackage) registerCompileResult.getAST()).globalVars.size(), 2,
                "Expected constants count not found." +
                        ((BLangPackage) registerCompileResult.getAST()).globalVars.size()
        );
        assertEquals(((BLangPackage) registerCompileResult.getAST()).services.size(), 1,
                "Expected services count not found. " +
                        ((BLangPackage) registerCompileResult.getAST()).services.size()
        );
        assertEquals(((BLangPackage) registerCompileResult.getAST()).getTypeDefinitions().size(), 1,
                "Expected type definitions count not found." +
                        ((BLangPackage) registerCompileResult.getAST()).getTypeDefinitions().size()
        );

        assertEquals(((BLangPackage) stubCompileResult.getAST()).globalVars.size(), 1,
                "Expected type definitions count not found." +
                        ((BLangPackage) registerCompileResult.getAST()).globalVars.size()
        );
        assertEquals(((BLangPackage) stubCompileResult.getAST()).getTypeDefinitions().size(), 2,
                "Expected type definitions count not found." +
                        ((BLangPackage) registerCompileResult.getAST()).getTypeDefinitions().size()
        );
        assertEquals(((BLangPackage) stubCompileResult.getAST()).constants.size(), 1,
                "Expected type definitions count not found." +
                        ((BLangPackage) registerCompileResult.getAST()).constants.size()
        );
    }

    private void validatePublicAttachedFunctions(CompileResult compileResult) {
        for (BLangFunction function : ((BLangPackage) compileResult.getAST()).functions) {
            if (function.attachedFunction) {
                assertTrue(function.getFlags().contains(Flag.PUBLIC), "Attached function " + function.getName() + "is" +
                        " not public");
            }
        }
    }

    private void validateAttachedResources(CompileResult compileResult, int resourceCount) {
        int attachedResourceCount = 0;
        for (BLangFunction function : ((BLangPackage) compileResult.getAST()).functions) {
            if (function.attachedFunction && function.getFlags().contains(Flag.RESOURCE)) {
                attachedResourceCount += 1;
            }
        }
        assertEquals(attachedResourceCount, resourceCount);
    }

    private void validateEnumNode(CompileResult compileResult) {
        for (BLangTypeDefinition typeDefinition : ((BLangPackage) compileResult.getAST()).typeDefinitions) {
            if ("sentiment".equals(typeDefinition.getName().value)) {
                assertEquals(((BLangFiniteTypeNode) typeDefinition.getTypeNode()).getValueSet().size(), 3,
                        "Type definition doesn't contain required value set");
                return;
            }
        }
        fail("Type definition for " + "sentiment" + " doesn't exist in compiled results");
    }

    private void validateConstantNode(CompileResult compileResult, String constantName) {
        for (BLangConstant constant : ((BLangPackage) compileResult.getAST()).constants) {
            if (constantName.equals(constant.getName().getValue())) {
                assertEquals(constant.symbol.type.tsymbol.name.value, "sentiment",
                        "Symbol value of the constant is not correct");
                return;
            }
        }
        fail("constant for " + constantName + " doesn't exist in compiled results");
    }

    private CompileResult getStubCompileResult(String protoFilename, Path outputDir, String outputFilename)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCmd1 = (GrpcCmd) grpcCmd.newInstance();
        Path protoFilePath = resourceDir.resolve(protoFilename);
        grpcCmd1.setBalOutPath(outputDir.toAbsolutePath().toString());
        grpcCmd1.setProtoPath(protoFilePath.toAbsolutePath().toString());
        grpcCmd1.execute();
        Path outputFilePath = outputDir.resolve(outputFilename);
        return BCompileUtil.compile(outputFilePath.toString());
    }

    private CompileResult getProxyCompileResult(String protoFilename, String testName)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> grpcCmd = Class.forName("org.ballerinalang.protobuf.cmd.GrpcCmd");
        GrpcCmd grpcCmd1 = (GrpcCmd) grpcCmd.newInstance();
        Path protoFilePath = resourceDir.resolve(protoFilename);
        grpcCmd1.setProtoPath(protoFilePath.toAbsolutePath().toString());
        grpcCmd1.setMode("proxy");
        Path proxyOutputDirPath = proxyServiceDirPath.resolve("src/" + testName);
        grpcCmd1.setBalOutPath(proxyOutputDirPath.toAbsolutePath().toString());
        grpcCmd1.execute();
        return BCompileUtil.compile(proxyServiceDirPath, testName, false, true);
    }

    @AfterClass
    public void clean() {
        BalFileGenerationUtils.delete(new File(TMP_DIRECTORY_PATH, protoExeName));
        BalFileGenerationUtils.delete(outputDirPath.toFile());
    }
}

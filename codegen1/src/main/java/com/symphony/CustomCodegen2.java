package com.symphony;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.java.SpringCodegen;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ENUMS_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.SchemaHandler.ONE_OF_PREFFIX;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

@Slf4j
public class CustomCodegen2 extends SpringCodegen {
  private Map<String, CodegenModel> oneOfModels = new HashMap<>();

  @Override
  public void processOpts() {
    super.processOpts();
    supportingFiles.add(new SupportingFile(
      "openApiDocConfig.mustache",
      (sourceFolder + File.separator + configPackage).replace(".", File.separator),
      "SwaggerDocumentationConfig.java"));
  }

  @Override
  public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
    Map<String, Object> results = super.postProcessAllModels(objs);

    // post process models to fix inheritance:
    // 1. for all childs, remove the OneOf<name> interface and replace it by the actual parent
    // 2. fix model classnames in discriminator mappings, for jackson type resolution
    oneOfModels.forEach((key, model) -> {
      String interfaceName = ONE_OF_PREFFIX + model.getName();
      Map<String, String> mapping = new HashMap<>(model.discriminator.getMapping());
      mapping.forEach((k, v) -> {
        String childModelName = OpenAPIUtil.getSimpleRef(v);
        CodegenModel childModel = (CodegenModel) ((List<Map>) ((Map) objs.get(childModelName)).get("models")).get(0).get("model");
        childModel.getInterfaceModels().removeIf(m -> interfaceName.equals(m.getName()));
        childModel.parent = model.classname;

        String type = toModelName(OpenAPIUtil.getSimpleRef(v));
        model.discriminator.getMapping().put(k, type);
      });
    });
    return results;
  }

  @Override
  public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
    return super.postProcessOperationsWithModels(objs, allModels);
  }

  @Override
  public CodegenProperty fromProperty(String name, Schema propertySchema) {
    return super.fromProperty(name, propertySchema);
  }

  @Override
  public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allDefinitions) {
    CodegenModel codegenModel = super.fromModel(name, schema, allDefinitions);

    if (schema instanceof ComposedSchema) {
      ComposedSchema composed = (ComposedSchema) schema;

      // keep track of composed models
      List<Schema> oneOf = composed.getOneOf();
      if (oneOf != null && !oneOf.isEmpty()) {
        if (codegenModel.discriminator != null && codegenModel.discriminator.getMapping() != null) {
          oneOfModels.put(name, codegenModel);
        }
      }

      // alternative allOf implementation, where the composed class contains the union of all fields
      List<Schema> allOf = composed.getAllOf();
      if (allOf != null && !allOf.isEmpty()) {
        Map<String, Schema> allProperties = new HashMap<>();
        List<String> allRequired = new ArrayList<>();

        for (int i = 0; i < allOf.size(); i++) {
          Schema interfaceSchema = allOf.get(i);
          if (StringUtils.isBlank(interfaceSchema.get$ref())) {
            continue;
          }
          Schema refSchema = null;
          String ref = OpenAPIUtil.getSimpleRef(interfaceSchema.get$ref());
          if (allDefinitions != null) {
            refSchema = allDefinitions.get(ref);
            if (refSchema == null) {
              // try to resolve by removing full path
              try {
                ref = Path.of(ref.split("#")[1]).getFileName().toString();
              } catch(Exception e) {
                log.info("failed to parse ref {}", ref);
              }
              refSchema = allDefinitions.get(ref);
            }
          }
          if (allDefinitions != null && refSchema != null) {
            addProperties(allProperties, allRequired, refSchema, allDefinitions);
          }
        }

        addVars(codegenModel, allProperties, allRequired);

        // fix enum properties for copied fields
        List<CodegenProperty> codegenProperties = codegenModel.vars;
        int count = 0, numVars = codegenProperties.size();
        for (CodegenProperty codegenProperty : codegenProperties) {
          count += 1;
          codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, count < numVars);

          boolean isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
          if (isEnum) {
            codegenModel.getVendorExtensions().put(HAS_ENUMS_EXT_NAME, true);
          }
        }
        codegenModel.vars = codegenProperties;

        boolean isEnum = getBooleanValue(codegenModel, IS_ENUM_EXT_NAME);
        if (!Boolean.TRUE.equals(isEnum)) {
          codegenModel.imports.add("JsonProperty");
          boolean hasEnums = getBooleanValue(codegenModel, HAS_ENUMS_EXT_NAME);
          if (Boolean.TRUE.equals(hasEnums)) {
            codegenModel.imports.add("JsonValue");
          }
        } else { // enum class
          //Needed imports for Jackson's JsonCreator
          if (additionalProperties.containsKey("jackson")) {
            codegenModel.imports.add("JsonCreator");
          }
        }
      }
    }

    return codegenModel;
  }
}

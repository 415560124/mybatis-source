/**
 *    Copyright ${license.git.copyrightYears} the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
public class XMLStatementBuilder extends BaseBuilder {

  private final MapperBuilderAssistant builderAssistant;
  private final XNode context;
  private final String requiredDatabaseId;

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
    this(configuration, builderAssistant, context, null);
  }

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context, String databaseId) {
    super(configuration);
    this.builderAssistant = builderAssistant;
    this.context = context;
    this.requiredDatabaseId = databaseId;
  }

  /**
   * 解析insert |delete |update | select节点，构建为{@link MappedStatement}对象
   * 添加到{@link Configuration#mappedStatements}属性中
   */
  public void parseStatementNode() {
    //获取insert |delete |update | select 语句的sqlId
    String id = context.getStringAttribute("id");
    /**
     * 判断节点是否配置了databaseId
     */
    String databaseId = context.getStringAttribute("databaseId");
    /**
     * 判断当前节点databaseId和数据源的数据库databaseId是否相等
     */
    if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
      return;
    }
    /**
     * insert |delete |update | select 获得节点名
     */
    String nodeName = context.getNode().getNodeName();
    /**
     * 通过nodeName，获得SqlCommandType枚举
     */
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    //判断是否为select节点
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    /**
     * 获取flushCache属性，刷新缓存。默认值：如果为查询节点，默认false。
     */
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    /**
     * 获取useCache属性，加载缓存。默认值：如果为查询节点，默认true
     */
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    /**
     * 获取resultOrder属性，是否需要处理嵌套查询结果（使用极少）
     * 可以将比如30条数据的三组数据  组成一个嵌套的查询结果
     */
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

    /**
     * 解析sql引用片段
     *     <select id="qryEmployeeById" resultType="Employee" parameterType="int">
     *       <include refid="selectInfo"></include>
     *       employee where id=#{id}
     *     </select>
     *     <include refid="selectInfo"></include> 解析成sql语句 放在<select>Node的子节点中
     */
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
    includeParser.applyIncludes(context.getNode());
    /**
     * 解析sql节点的参数类型
     */
    String parameterType = context.getStringAttribute("parameterType");
    //把参数类型字符串转为Class
    Class<?> parameterTypeClass = resolveClass(parameterType);
    /**
     * 看sql是否支持自定义脚本语言，就是用来判断的那个<if test=""></if>
     */
    String lang = context.getStringAttribute("lang");
    /**
     * 获得自定义sql脚本语言驱动，默认：{@link org.apache.ibatis.scripting.xmltags.XMLLanguageDriver}
     */
    LanguageDriver langDriver = getLanguageDriver(lang);

    /**
     * 解析insert语句的selectKey节点
     */
    processSelectKeyNodes(id, parameterTypeClass, langDriver);

    /**
     * 解析主键生成策略
     */
    KeyGenerator keyGenerator;
    //生成主键唯一id
    String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
    //判断配置类中是否包含或者已经解析过主键生成器
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    } else {
      //读取insert中的主键生成策略 useGeneratedKeys
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
          ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }
    /**
     * 通过{@link XMLLanguageDriver}来解析sql脚本对象，解析为SqlNode。
     * 只是解析成一个个SqlNode，并不会完全解析sql，因为这个时候参数都没确定，动态sql无法解析
     */
    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
    /**
     * 解析statementType属性
     * 1、STATEMENT：直接操作sql，不进行预编译，获取数据：$--Statement
     * 2、PREPARED：预处理，参数，进行预编译，获取数据：#--PreparedStatement：默认
     * 3、CALLABLE：执行存储过程—CallableStatement
     */
    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    /**
     * 解析fetchSize属性
     * 作用：通过JDBC取数据时，默认是10条数据取一次，即fetch size为10。调大可以有效减少客户端与数据库的往返时间
     */
    Integer fetchSize = context.getIntAttribute("fetchSize");
    /**
     * 解析timeout属性：超时时间
     */
    Integer timeout = context.getIntAttribute("timeout");
    /**
     * 解析parameterMap属性：参数映射
     */
    String parameterMap = context.getStringAttribute("parameterMap");
    /**
     * 解析resultType属性：返回类型
     */
    String resultType = context.getStringAttribute("resultType");
    /**
     * 解析返回类型为Class
     */
    Class<?> resultTypeClass = resolveClass(resultType);
    /**
     * 解析resultMap属性：返回映射
     */
    String resultMap = context.getStringAttribute("resultMap");
    /**
     * 解析resultSetType属性
     * ResultSet.TYPE_FORWORD_ONLY 结果集的游标只能向下滚动。
     * ResultSet.TYPE_SCROLL_INSENSITIVE 结果集的游标可以上下移动，当数据库变化时，当前结果集不变。
     * ResultSet.TYPE_SCROLL_SENSITIVE 返回可滚动的结果集，当数据库变化时，当前结果集同步改变。
     */
    String resultSetType = context.getStringAttribute("resultSetType");
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
    if (resultSetTypeEnum == null) {
      resultSetTypeEnum = configuration.getDefaultResultSetType();
    }
    /**
     * 解析keyProperty属性：实体类主键字段
     */
    String keyProperty = context.getStringAttribute("keyProperty");
    /**
     * 解析keyColumn属性：数据库主键字段
     */
    String keyColumn = context.getStringAttribute("keyColumn");
    String resultSets = context.getStringAttribute("resultSets");
    /**
     * 为insert|delete|update|select构建{@link MappedStatement}对象
     * 添加到{@link Configuration#mappedStatements}
     */
    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
  }

  private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
    List<XNode> selectKeyNodes = context.evalNodes("selectKey");
    if (configuration.getDatabaseId() != null) {
      parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
    }
    parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
    removeSelectKeyNodes(selectKeyNodes);
  }

  private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
    for (XNode nodeToHandle : list) {
      String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
      String databaseId = nodeToHandle.getStringAttribute("databaseId");
      if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
        parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
      }
    }
  }

  private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
    String resultType = nodeToHandle.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
    String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
    boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

    // defaults
    boolean useCache = false;
    boolean resultOrdered = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;

    SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);

    id = builderAssistant.applyCurrentNamespace(id, false);

    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
  }

  private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
    for (XNode nodeToHandle : selectKeyNodes) {
      nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
    }
  }

  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      return requiredDatabaseId.equals(databaseId);
    }
    if (databaseId != null) {
      return false;
    }
    id = builderAssistant.applyCurrentNamespace(id, false);
    if (!this.configuration.hasStatement(id, false)) {
      return true;
    }
    // skip this statement if there is a previous one with a not null databaseId
    MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
    return previous.getDatabaseId() == null;
  }

  private LanguageDriver getLanguageDriver(String lang) {
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = resolveClass(lang);
    }
    return configuration.getLanguageDriver(langClass);
  }

}

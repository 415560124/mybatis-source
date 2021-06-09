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
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.builder.StaticSqlSource;

/**
 * 动态SQL解析
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  /**
   * 动态sql需要解析，rootSqlNode属性下的所有{@link SqlNode}，调用{@link SqlNode#apply(DynamicContext)}方法
   * @param parameterObject
   * @return
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    /**
     * 封装为动态SQL上下文
     * {@link DynamicContext#bindings}参数
     * {@link DynamicContext#sqlBuilder}拼接的SQL语句
     */
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    //拼接SQL，会替换${}，但不会替换#{}
    rootSqlNode.apply(context);
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    /**
     * 解析'#{}'并替换为'?'
     * 并包装参数到{@link StaticSqlSource#parameterMappings}
     * 重新包装为SqlSource为{@link StaticSqlSource}
     */
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    //返回BoundSql对象
    return boundSql;
  }

}

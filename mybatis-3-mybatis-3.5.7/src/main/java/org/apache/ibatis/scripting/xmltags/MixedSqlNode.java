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

import java.util.List;

/**
 * 混合SqlNode，存储了多个其他SqlNode
 * @author Clinton Begin
 */
public class MixedSqlNode implements SqlNode {
  private final List<SqlNode> contents;

  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  /**
   * 循环所有SqlNode节点，调用{@link SqlNode#apply(DynamicContext)}
   * @param context
   * @return
   */
  @Override
  public boolean apply(DynamicContext context) {
    //循环所有SqlNode节点，调用{@link SqlNode#apply(DynamicContext)}
    contents.forEach(
      node -> {
        node.apply(context);
    });
    return true;
  }
}

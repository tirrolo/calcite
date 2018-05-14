package org.apache.calcite.adapter.jdbc;

import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.schema.*;

import java.util.Collection;
import java.util.Set;

/**
 * Extends JdbcSchema
 */
public class SchemaDecoratorDavide implements Schema {


  private final Schema decorated;


  public SchemaDecoratorDavide(Schema decorated){
    this.decorated = decorated;
  }

  public Schema getDecorated(){
    return this.decorated;
  }

  @Override
  public Table getTable(String name) {
    return decorated.getTable(name);
  }

  @Override
  public Set<String> getTableNames() {
    return decorated.getTableNames();
  }

  @Override
  public Collection<Function> getFunctions(String name) {
    return decorated.getFunctions(name);
  }

  @Override
  public Set<String> getFunctionNames() {
    return decorated.getFunctionNames();
  }

  @Override
  public Schema getSubSchema(String name) {
    return decorated.getSubSchema(name);
  }

  @Override
  public Set<String> getSubSchemaNames() {
    return decorated.getSubSchemaNames();
  }

  @Override
  public Expression getExpression(SchemaPlus parentSchema, String name) {
    return decorated.getExpression(parentSchema, name);
  }

  @Override
  public boolean isMutable() {
    return decorated.isMutable();
  }

  @Override
  public Schema snapshot(SchemaVersion version) {
    return decorated.snapshot(version);
  }


};

/*
 * Copyright 2014 Databricks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.dnetlib.iis.common.spark.avro

import java.sql.Timestamp
import java.util.HashMap
import scala.collection.immutable.Map
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.AvroJob
import org.apache.avro.mapred.AvroKey
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.ArrayType
import org.apache.spark.sql.types.BinaryType
import org.apache.spark.sql.types.BooleanType
import org.apache.spark.sql.types.ByteType
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.DecimalType
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.sql.types.FloatType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.types.MapType
import org.apache.spark.sql.types.ShortType
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.TimestampType
import org.apache.avro.mapred.AvroOutputFormat
import org.apache.avro.mapred.AvroWrapper


/** 
 * Based on com.databricks.spark.avro.AvroSaver. This version takes avroSchema as a parameter
 * to the save method - thanks to that the schema is not a generic one ([[org.apache.avro.generic.GenericRecord]])
 * generated from a data frame schema   
 *  
 * This object provides a save() method that is used to save DataFrame as avro file.
 * To do this, we first convert the schema and then convert each row of the RDD to corresponding
 * avro types. One remark worth mentioning is the structName parameter that functions have. Avro
 * records have a name associated with them, which must be unique. Since SturctType in sparkSQL
 * doesn't have a name associated with it, we are taking the name of the last structure field that
 * the current structure is a child of. For example if the row at the top level had a field called
 * "X", which happens to be a structure, we would call that structure "X". When we process original
 * rows, they get a name "topLevelRecord".
 */
object AvroSaver {

  def save(dataFrame: DataFrame, avroSchema: Schema, location: String): Unit = {
    val jobConf = new JobConf(dataFrame.sqlContext.sparkContext.hadoopConfiguration)
    val builder = SchemaBuilder.record("topLevelRecord")
    val schema = dataFrame.schema
    AvroJob.setOutputSchema(jobConf, avroSchema)

    dataFrame.mapPartitions(rowsToAvro(_, schema)).saveAsHadoopFile(location,
      classOf[AvroWrapper[GenericRecord]],
      classOf[NullWritable],
      classOf[AvroOutputFormat[GenericRecord]],
      jobConf)
  }

  private def rowsToAvro(
      rows: Iterator[Row],
      schema: StructType): Iterator[(AvroKey[GenericRecord], NullWritable)] = {
    val converter = createConverter(schema, "topLevelRecord")
    rows.map(x => (new AvroKey(converter(x).asInstanceOf[GenericRecord]),
      NullWritable.get())).toIterator
  }

  /**
   * This function constructs converter function for a given sparkSQL datatype. These functions
   * will be used to convert dataFrame to avro format.
   */
  def createConverter(dataType: DataType, structName: String): (Any) => Any = {
    dataType match {
      case ByteType | ShortType | IntegerType | LongType | FloatType | DoubleType | StringType |
           BinaryType | BooleanType =>
        (item: Any) => item

      case _: DecimalType =>
        (item: Any) => if (item == null) null else item.toString

      case TimestampType =>
        (item: Any) => {
          if (item == null) null else item.asInstanceOf[Timestamp].getTime
        }

      case ArrayType(elementType, _) =>
        val elementConverter = createConverter(elementType, structName)

        (item: Any) => {
          if (item == null) {
            null
          } else {
            val sourceArray = item.asInstanceOf[Seq[Any]]
            val sourceArraySize = sourceArray.size
            val targetArray = new Array[Any](sourceArraySize)
            var idx = 0

            while (idx < sourceArraySize) {
              targetArray(idx) = elementConverter(sourceArray(idx))
              idx += 1
            }

            targetArray
          }
        }

      case MapType(StringType, valueType, _) =>
        val valueConverter = createConverter(valueType, structName)

        (item: Any) => {
          if (item == null) {
            null
          } else {
            val javaMap = new HashMap[String, Any]()
            item.asInstanceOf[Map[String, Any]].foreach { case (key, value) =>
              javaMap.put(key, valueConverter(value))
            }
            javaMap
          }
        }

      case structType: StructType =>
        val builder = SchemaBuilder.record(structName)
        val schema: Schema = SchemaConverters.convertStructToAvro(
          structType, builder)
        val fieldConverters = structType.fields.map(field =>
          createConverter(field.dataType, field.name))

        (item: Any) => {
          if (item == null) {
            null
          } else {
            val record = new Record(schema)
            val convertersIterator = fieldConverters.iterator
            val fieldNamesIterator = dataType.asInstanceOf[StructType].fieldNames.iterator
            val rowIterator = item.asInstanceOf[Row].toSeq.iterator

            while (convertersIterator.hasNext) {
              val converter = convertersIterator.next
              record.put(fieldNamesIterator.next, converter(rowIterator.next))
            }
            record
          }
        }
    }
  }
}

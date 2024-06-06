package wtf.imba.inspector.hooks

import android.app.Activity
import android.content.ContentValues
import android.content.ContextWrapper
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.java.AnyArrayClass
import com.highcapable.yukihookapi.hook.type.java.FileClass
import com.highcapable.yukihookapi.hook.type.java.StringArrayClass
import com.highcapable.yukihookapi.hook.type.java.StringClass
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.File





object SQLiteHook : YukiBaseHooker() {

    const val TAG = "SQLiteHook"
    override fun onHook() {
        databaseHook()
    }

    private fun databaseHook() {
        SQLiteDatabase::class.java.hook {
            injectMember {
                method {
                    name = "execSQL"
                    returnType = StringClass
                }

                afterHook {
                    XposedBridge.log(TAG + "execSQL(" + this.args[0] + ")")
                    Log.d(TAG, "databaseHook: execSQL ${this.args[0]}")
                }
            }
        }


        SQLiteDatabase::class.java.hook {
            injectMember {
                method {
                    name = "execSQL"
                    returnType = StringClass
                    param(AnyArrayClass)
                }
                afterHook {
                    val obj = this.args[1] as Array<*>?
                    var obj_c = 0
                    if (obj != null && obj.isNotEmpty()) {
                        obj_c = obj.size
                    }

                    XposedBridge.log(TAG + "execSQL(" + this.args[0] + ") with " + obj_c.toString() + " args.")
                }
            }
        }

        SQLiteDatabase::class.java.hook {
            injectMember {
                method {
                    name = "update"
                    returnType = StringClass
                    param(ContentValues::class.java, StringClass, StringArrayClass)
                }
                afterHook {
                    val sqlitedb: SQLiteDatabase = this.instance()
                    val contentValues : ContentValues = this.args[1] as ContentValues
                    val sb = StringBuffer()
                    val set: Set<Map.Entry<String, Any>> = contentValues.valueSet()
                    for (entry: Map.Entry<String, Any> in set) {
                        sb.append(entry.key + "=" + entry.value.toString() + ",")
                    }

                    val sbuff = StringBuffer()
                    if (this.args[3] != null) {
                        for (str: String in  this.args[3] as Array<String>) {
                            sbuff.append("$str,")
                        }
                    }

                    var str = ""
                    if (sb.toString().length > 1) {
                        str = sb.toString().substring(0, sb.length - 1)
                    }

                    var whereArgs = ""
                    if (sbuff.toString().length > 1) {
                        whereArgs = sbuff.toString().substring(0, sbuff.length - 1)
                    }

                    XposedBridge.log(TAG + "\nUPDATE " + this.args[0] + " SET " + str + "" +
                            " WHERE " + this.args[2] + "" + whereArgs)
                }
            }
        }

        SQLiteDatabase::class.java.hook {
            injectMember {
                method {
                    name = "insert"
                    returnType = StringClass
                }

                afterHook {
                    val sqlitedb: SQLiteDatabase = this.instance()
                    val contentValues : ContentValues = this.args[2] as ContentValues
                    val sb = StringBuffer()
                    for (entry: Map.Entry<String, Any> in contentValues.valueSet()) {
                        sb.append(entry.key + "=" + entry.value.toString() + ",")
                    }
                    XposedBridge.log(TAG + "INSERT INTO " + this.args[0] + " VALUES(" + sb.toString().substring(0, sb.length - 1) + ")")
                }
            }
        }

        Activity::class.java.hook {
            injectMember {
                method {
                    name = "managedQuery"
                    returnType = Uri::class
                    param(StringArrayClass, StringClass, StringArrayClass, StringClass)
                }
                afterHook {
                    val uri = this.args[0] as Uri

                    val projection = StringBuffer()
                    if (this.args[1] != null) {
                        for (str in this.args[1] as Array<String>) {
                            projection.append("$str,")
                        }
                    }


                    var selection = ""
                    if (this.args[2] != null) {
                        selection = " WHERE " + this.args[2] as String + " = "
                    }

                    val selectionArgs = StringBuffer()
                    if (this.args[3] != null) {
                        for (str in this.args[3] as Array<String>) {
                            selectionArgs.append("$str,")
                        }
                    }

                    var sortOrder = ""
                    if (this.args[4] != null) {
                        sortOrder = " ORDER BY " + this.args[4] as String
                    }

                    var projec = ""
                    projec = if (projection.toString() == "") {
                        "*"
                    } else {
                        projection.toString().substring(0, projection.length - 1)
                    }


                    val cursor = this.result as Cursor?

                    val result = StringBuffer()

                    if (cursor != null) if (cursor.moveToFirst()) {
                        do {
                            val x = cursor.columnCount
                            val sb = StringBuffer()
                            for (i in 0 until x) {
                                if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                                    val blob: String =
                                        Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP)
                                    sb.append(cursor.getColumnName(i) + "=" + blob + ",")
                                } else {
                                    sb.append(cursor.getColumnName(i) + "=" + cursor.getString(i) + ",")
                                }
                            }
                            result.append(
                                """
                ${sb.toString().substring(0, sb.length - 1)}
                
                """.trimIndent()
                            )
                        } while (cursor.moveToNext())
                    }

                    XposedBridge.log(TAG + "SELECT " + projec + " FROM " + uri.authority + uri.path +
                            selection + selectionArgs.toString() + sortOrder + "\n   [" + result.toString() + "]")
                }
            }
        }

        SQLiteDatabase::class.java.hook {
            injectMember {
                method {
                    name = "query"
                    returnType = StringClass
                    param(
                        StringArrayClass, StringClass,
                        StringArrayClass, StringClass, StringClass, StringClass, StringClass)
                }
                afterHook {
                    this.apply {
                        val table = args[0] as String
                        val columns = args[1] as Array<String>
                        val having = args[5] as String
                        val limit = args[6] as String

                        val csb = StringBuffer()
                        if (args[1] != null) {
                            for (str in args[1] as Array<String>) {
                                csb.append("$str,")
                            }
                        }

                        var selection = ""
                        if (args[2] != null) {
                            selection = " WHERE " + args[2] as String + " = "
                        }

                        val selectionArgs = StringBuffer()
                        if (args[3] != null) {
                            for (str in args[3] as Array<String>) {
                                selectionArgs.append("$str,")
                            }
                        }

                        var groupBy = ""
                        if (args[4] != null) {
                            groupBy = " GROUP BY " + args[4] as String
                        }

                        var sortOrder = ""
                        if (args[6] != null) {
                            sortOrder = " ORDER BY " + args[6] as String
                        }

                        if (csb.toString() == "") {
                            csb.append("*")
                        }

                        val cursor = result as Cursor

                        val result = StringBuffer()

                        if (cursor != null) if (cursor.moveToFirst()) {
                            do {
                                val x = cursor.columnCount
                                val sb = StringBuffer()
                                for (i in 0 until x) {
                                    if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                                        val blob =
                                            Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP)
                                        sb.append(cursor.getColumnName(i) + "=" + blob + ",")
                                    } else {
                                        sb.append(cursor.getColumnName(i) + "=" + cursor.getString(i) + ",")
                                    }
                                }
                                result.append(
                                    """
                ${sb.toString().substring(0, sb.length - 1)}
                
                """.trimIndent()
                                )
                            } while (cursor.moveToNext())
                        }

                        XposedBridge.log(TAG + "SELECT " + csb.toString() + " FROM " + table +
                                selection + selectionArgs.toString() + sortOrder + "\n" + result.toString() + "")
                    }
                }
            }
        }

        ContextWrapper::class.java.hook {
            injectMember {
                method {
                    name = "getDatabasePath"
                    returnType = StringClass
                }
                afterHook {
                    XposedBridge.log(TAG + "[Context] getDatabasePath(" + this.args[0] + ")")
                }
            }
        }

        ///SQLCipher
        try {
            "net.sqlcipher.database.SQLiteDatabase".hook {
                injectMember {
                    method {
                        name = "execSQL"
                        returnType = StringClass
                    }
                    afterHook {
                        XposedBridge.log(TAG + "[SQLCipher] execSQL(" + this.args[0] + ")")
                    }
                }
            }

            "net.sqlcipher.database.SQLiteDatabase".hook {
                injectMember {
                    method {
                        name = "execSQL"
                        returnType = StringClass
                        param(AnyArrayClass)
                    }
                    afterHook {
                        val obj = this.args[1] as Array<Any>?
                        var obj_c = 0
                        if (!obj.isNullOrEmpty()) {
                            obj_c = obj.size
                        }
                        XposedBridge.log(TAG + "[SQLCipher] execSQL(" + this.args[0] + ") with " + obj_c.toString() + " args.")
                    }
                }
            }

            "net.sqlcipher.database.SQLiteDatabase".hook {
                injectMember {
                    method {
                        name = "openOrCreateDatabase"
                        returnType = FileClass
                        param(StringClass, "net.sqlcipher.database.SQLiteDatabase.CursorFactory")
                    }
                    afterHook {
                        val f = this.args[0] as File
                        val passwd = this.args[1] as String
                        XposedBridge.log(TAG + "[SQLCipher] Open or Create:" + f.name + " with password: " + passwd)
                    }
                }
            }

            "net.sqlcipher.database.SQLiteDatabase".hook {
                injectMember {
                    method {
                        name = "rawQuery"
                        returnType = StringClass
                        param(StringArrayClass)
                    }
                    afterHook {
                        val obj = this.args[1] as Array<String>?
                        var obj_c = 0
                        if (!obj.isNullOrEmpty()) {
                            obj_c = obj.size
                        }
                        XposedBridge.log(TAG + "[SQLCipher] rawQuery(" + this.args[0] + ") with " + (obj_c).toString() + " args.")
                    }
                }
            }
        } catch (e: XposedHelpers.ClassNotFoundError) {
            Log.d(TAG, "databaseHook: ${e.message}")
        }

    }


}
package template.fileRetrieval

/**
  * Created by Ilya Volynin on 16.12.2019 at 12:32.
  */
case class FileStorage(baseDir: String)

case class FSConfig(url: String, fileStorage: FileStorage)

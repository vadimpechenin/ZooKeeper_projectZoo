package zoo

import org.apache.zookeeper._
// Необходимая библеотека
import scala.util.Random
// Реализация класса Animal

case class Animal(name:String, hostPort:String,
                  root:String, partySize:Integer) extends Watcher {
  // Инициализация Animal заключается в
  //установлении соединения с ZooKeeper, определении переменных mutex и
  //animalPath.
  val zk = new ZooKeeper(hostPort, 3000, this)
  val mutex = new Object()
  val animalPath = root+"/"+name

  // Реализация методов
  //process интерфейса Watcher.
  override def process(event: WatchedEvent): Unit = {
    // код реакции на событие
    // Блок синхронизации
    mutex.synchronized {
      // Простая реакция - вывод на экран события
      println(s"Event from keeper: ${event.getType}")
      // Добавленная строка чтобы все работало
      mutex.notify()
    }
  }

  // Реализация метода enter
  def enter():Boolean = {
    // код создания узла и ожидания у барьера
    // создание эфимерного узла


    zk.create(animalPath, Array.emptyByteArray,
      ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL)

    println("Создан эфимерный узел:" + animalPath);
    // Блок, ожидающий появления в корневом узле /zoo всех животных
    mutex.synchronized {
      while (true) {
        val party = zk.getChildren(root, this)
        println("Размер группы:"+party.size())
        if (party.size() < partySize) {
          println("Waiting for the others.")
          mutex.wait()
          println("Noticed someone.")
        } else {
          return true
        }
      }
    }
    return false
  }

  // метод удаления эфимерного узла
  def leave():Unit = {
    println("Удален узел: " + animalPath);
    zk.delete(animalPath,-1)
    println("Deleted")
  }

  if (zk == null) throw new Exception("ZK is NULL.")
}

object Main {

  val sleepTime = 1

  def main(args: Array[String]): Unit = {
    println("Starting animal runner")
    // Программа ожидает в
    //качестве аргументов список: имя животного, адрес и порт zookeeper, размер
    //группы животных
    val Seq(animalName, hostPort, partySize) =
      args.toSeq
    // оздание объекта Animal на основе
    //параметров: имя животного, адрес и порт zookeeper, путь к корневому узлу
    //для узлов животных, величина группы животных
    val animal = Animal(animalName, hostPort, "/zoo",
      partySize.toInt)
    // Код взаимодействия с ZooKeeper
    try {
      // Связь объекта с ZooKeeper, создание
      //эфимерного узела с именем animalName и подписываться на обновления
      //группы /zoo
      animal.enter()
      println(s"${animal.name} entered.")
      // Cообщение о работе процесса
      for (i <- 1 to Random.nextInt(100)) {
        Thread.sleep(sleepTime)
        println(s"${animal.name} is running...")
      }
      animal.leave()

    } catch {
      case e: Exception => println("Animal was not permitted to the zoo." + e)
    }
  }
}

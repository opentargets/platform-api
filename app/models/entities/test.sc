

case class MyObject[T,S,R](f1: T, f2: S, f3: R)

val myObjects: Seq[MyObject[Int, Double, String]] = ???

myObjects.unzip3(x => (x.f1, x.f2, x.f3))
val (l1, l2, l3) = myObjects.foldLeft((List.empty[Int], List.empty[Double], List.empty[String]))((acc, nxt) => {
  (nxt.f1 :: acc._1, nxt.f2 :: acc._2, nxt.f3 :: acc._3)
})
package dev.example.eventsourcing.domain

import scalaz._

trait Update[A] {
  def apply(events: List[Event] = Nil): (List[Event], DomainValidation[A])

  def map[B](f: A => B) = Update { events =>
    this(events) match {
      case (updatedEvents, Success(result)) => (updatedEvents, Success(f(result)))
      case (updatedEvents, Failure(errors)) => (updatedEvents, Failure(errors))
    }
  }

  def flatMap[B](f: A => Update[B]) = Update { events =>
    this(events) match {
      case (updatedEvents, Success(result)) => f(result)(updatedEvents)
      case (updatedEvents, Failure(errors)) => (updatedEvents, Failure(errors))
    }
  }

  def result(onSuccess: (List[Event], A) => Unit = (e, r) => ()): DomainValidation[A] = {
    val (events, validation) = apply()
    validation match {
      case Success(result) => { onSuccess(events, result); Success(result) }
      case failure         => failure
    }
  }
}

object Update {
  def apply[A](f: List[Event] => (List[Event], DomainValidation[A])) = new Update[A] {
    def apply(events: List[Event]) = f(events)
  }

  def accept[A](result: A) =
    Update(events => (events, Success(result)))

  def accept[A](event: Event, result: A) =
    Update(events => (event :: events, Success(result)))

  def reject[A](errors: DomainError) =
    Update[A](events => (events, Failure(errors)))
}
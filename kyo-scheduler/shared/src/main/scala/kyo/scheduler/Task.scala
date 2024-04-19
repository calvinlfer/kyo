package kyo.scheduler

trait Task(initialRuntime: Int):

    @volatile private var state = Math.max(1, initialRuntime) // Math.abs(state) => runtime; state < 0 => preempting

    private[kyo] def doRun(clock: Clock): Task.Result =
        val start = clock.currentMillis()
        try run(start, clock)
        finally state = (Math.abs(state) + clock.currentMillis() - start).toInt
    end doRun

    def doPreempt(): Unit =
        if state > 0 then
            state = -state

    final def preempt(): Boolean =
        state < 0

    def run(startMillis: Long, clock: Clock): Task.Result

    def runtime(): Int = Math.abs(state)
end Task

private[kyo] object Task:

    private val ordering = new Ordering[Task]:
        override def lt(x: Task, y: Task): Boolean =
            val r = x.runtime()
            r == 0 || r < y.runtime()
        def compare(x: Task, y: Task): Int =
            y.state - x.state

    inline given Ordering[Task] = ordering

    opaque type Result = Boolean
    val Preempted: Result = true
    val Done: Result      = false
    object Result:
        given CanEqual[Result, Result] = CanEqual.derived

    inline def apply(inline r: => Unit): Task =
        new Task(0):
            def run(startMillis: Long, clock: Clock) =
                r
                Task.Done
            end run
end Task

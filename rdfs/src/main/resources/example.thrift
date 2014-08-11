struct Point {
    1: double x
}

struct Elem {
    1: i32 x
}

struct Missing {
    1: bool y = true
    2: list<string> aa
    3: Elem i
    4: set<bool> bb
    5: map<string, string> cc
}

union Thing {
    1: Point a
    2: Elem b
    3: Missing c
}

union OtherThig {
    1: Elem b
    2: string c
    3: Missing d
}

enum Day {
    Monday,
    Tuesday,
    Wednesday
}

typedef Day D

service Heartbeet {
    string ping(1: string greet, 2: Thing b, 4: D day)
}
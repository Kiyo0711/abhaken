package io.github.kiyo0711.abhaken;

/** Ein Eintrag der Liste. done == 0 heißt offen, sonst Zeitpunkt des Abhakens. */
final class Todo {
    String id;
    String text;
    long created;
    long done;
}

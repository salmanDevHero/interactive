package com.example.interactive

import kotlin.random.Random

// Define the Particle class
private class Particle(var x: Float, var y: Float) {
    var vx: Float = Random.nextFloat() * 0.02f - 0.01f
    var vy: Float = Random.nextFloat() * 0.02f - 0.01f
    var size: Float = Random.nextFloat() * 15f + 5f
}
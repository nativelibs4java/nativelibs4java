/***********************************************************************

 Copyright (c) 2008, 2009, Memo Akten, www.memo.tv
 *** The Mega Super Awesome Visuals Company ***
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of MSA Visuals nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***********************************************************************/ 

// Ported to JavaCL/OpenCL4Java by Olivier Chafik

#define REPULSION_FORCE 4.0f
#define CENTER_FORCE2 0.0005f

#define PI 3.141f

__kernel void updateParticle(
        __global float* masses,
        __global float2* velocities,
        //__global Particle* particles,
        __global float4* particles,
        //__global char* pParticles,
        const float2 mousePos,
        const float2 dimensions,
        float massFactor,
        float speedFactor,
        float slowDownFactor,
        float mouseWeight,
        char limitToScreen
) {
	int id = get_global_id(0);

        float4 particle = particles[id];
        uchar4 color = *(uchar4*)&particle.x;

        float2 position = particle.yz;
	float2 diff = mousePos - position;

        float invDistSQ = 1.0f / dot(diff, diff);
	diff *= 200.0f * invDistSQ;

        float mass = massFactor * masses[id];
        float2 velocity = velocities[id];
	velocity -= mass * position * CENTER_FORCE2 - diff * mass * mouseWeight;// + diff * mass * mouseWeight * invDistSQ / 100;
        //velocities[id] -= position * CENTER_FORCE2 - mass * diff * (invDistSQ - (float2)(1, 1));
        position += speedFactor * velocities[id];
        
        if (limitToScreen) {
            float2 halfDims = dimensions / 2;
            position = clamp(position, -halfDims, halfDims);
            //float2 clamped = clamp(position, -halfDims, halfDims);
            //if (position.x != clamped.x || position.y != clamped.y)
            //    slowDownFactor /= (distance(position, clamped) / 10 + 1);

            //position = clamped;
        }

        float dirDot = cross((float4)(diff, (float2)0), (float4)(velocity, (float2)0)).z;

        //float angle = (atan2(velocity.y, velocity.x) + PI) / (2 * PI);
        
/*
        float adirDot = dirDot < 0 ? -dirDot : dirDot;
        if (adirDot > 1)
            adirDot = 1;
        
        float dev = adirDot * 0.3f;
        float fx = 0.5f + dev;
        float fy = 0.2f + angle * 0.6f;
        float fz = 0.5f - dev;

        color.x = (uchar)(fx * 255);
        color.y = (uchar)(fy * 255);
        color.z = (uchar)(fz * 255);
*/
        float speed = length(velocity);

        float f = speed / 4 / mass;
        float hue = (dirDot < 0 ? f : f + 1) / 2;
        hue = clamp(hue, 0.0f, 1.0f) * 360;

        float opacity = clamp(0.1f + f, 0.0f, 1.0f);
        float saturation = mass / 2;
        float brightness = 0.6f + opacity * 0.3f;

        uchar4 targetColor = HSVAtoRGBA((float4)(hue, saturation, brightness, opacity));
        
        float colorSpeedFactor = min(0.01f * speedFactor, 1.0f), otherColorSpeedFactor = 1 - colorSpeedFactor;
        color = (uchar4)(
            (uchar)(targetColor.x * colorSpeedFactor + color.x * otherColorSpeedFactor),
            (uchar)(targetColor.y * colorSpeedFactor + color.y * otherColorSpeedFactor),
            (uchar)(targetColor.z * colorSpeedFactor + color.z * otherColorSpeedFactor),
            (uchar)(targetColor.w * colorSpeedFactor + color.w * otherColorSpeedFactor)
        );
        
        particle.yz = position;
        *(uchar4*)&particle.x = color;

	particles[id] = particle;

        velocity *= slowDownFactor;
        velocities[id] = velocity;
}



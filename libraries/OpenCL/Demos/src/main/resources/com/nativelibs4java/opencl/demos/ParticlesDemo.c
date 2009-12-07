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

#define DAMP 0.8f
#define DAMP2 0.8f
#define MIN_DIST 50
#define MIN_DIST_SQ MIN_DIST*MIN_DIST

#define REPULSION_FORCE 4.0f
#define CENTER_FORCE2 8.0f

#define MIN_SPEED2 100

__kernel void updateParticle(
		__global float* masses, 
		__global float2* velocities,
		__global float2* pOut, 
		const float2 mousePos, 
		const float2 dimensions
) {
	int id = get_global_id(0);
 
	float2 diff = mousePos - pOut[id];
	float invDistSQ = 1.0f / dot(diff, diff);
	diff *= 300.0f * invDistSQ;
 
	velocities[id] += (dimensions*0.5 - pOut[id]) * CENTER_FORCE2 - diff* masses[id];
	pOut[id] += velocities[id];
	velocities[id] *= DAMP2;
 
	float speed2 = dot(velocities[id], velocities[id]);
	if (speed2 < MIN_SPEED2) 
		pOut[id] = mousePos + diff * (1 + masses[id]);
}

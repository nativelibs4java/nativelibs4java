package org.bridj;
import org.bridj.ann.*;
import java.util.List;

public class TimeT {
	
	@Struct(customizer = timeval_customizer.class)
	public class timeval extends StructObject {
		 
		@Field(0) 
		public long seconds() {
			return this.io.getCLongField(this, 0);
		}
		@Field(0) 
		public timeval seconds(long seconds) {
			this.io.setCLongField(this, 0, seconds);
			return this;
		}
		public final long seconds_$eq(long seconds) {
			seconds(seconds);
			return seconds;
		}
		@Field(1) 
		public int milliseconds() {
			return this.io.getIntField(this, 1);
		}
		@Field(1) 
		public timeval milliseconds(int milliseconds) {
			this.io.setIntField(this, 1, milliseconds);
			return this;
		}
		public final int milliseconds_$eq(int milliseconds) {
			milliseconds(milliseconds);
			return milliseconds;
		}
	}

	public static class timeval_customizer extends StructIO.DefaultCustomizer {
		@Override
		public void beforeLayout(StructIO io, List<StructIO.AggregatedFieldDesc> aggregatedFields) {
			StructIO.AggregatedFieldDesc secondsField = aggregatedFields.get(0);
			if (Platform.isWindows())
				secondsField.byteLength = 4;
			else
				secondsField.byteLength = 8;
			
			secondsField.alignment = secondsField.byteLength;
		}
	}
}

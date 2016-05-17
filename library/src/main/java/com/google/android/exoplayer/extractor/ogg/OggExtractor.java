/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer.extractor.ogg;

import com.google.android.exoplayer.ParserException;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorInput;
import com.google.android.exoplayer.extractor.ExtractorOutput;
import com.google.android.exoplayer.extractor.PositionHolder;
import com.google.android.exoplayer.extractor.TrackOutput;
import com.google.android.exoplayer.util.ParsableByteArray;

import java.io.IOException;

/**
 * Ogg {@link Extractor}.
 */
public class OggExtractor implements Extractor {

  private StreamReader streamReader;

  @Override
  public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
    try {
      ParsableByteArray scratch = new ParsableByteArray(new byte[OggUtil.PAGE_HEADER_SIZE], 0);
      OggUtil.PageHeader header = new OggUtil.PageHeader();
      if (!OggUtil.populatePageHeader(input, header, scratch, true)
          || (header.type & 0x02) != 0x02) {
        return false;
      }
      input.peekFully(scratch.data, 0, header.bodySize);
      scratch.setLimit(header.bodySize);
      if (FlacReader.verifyBitstreamType(resetPosition(scratch))) {
        streamReader = new FlacReader();
      } else if (VorbisReader.verifyBitstreamType(resetPosition(scratch))) {
        streamReader = new VorbisReader();
      } else if (OpusReader.verifyBitstreamType(resetPosition(scratch))) {
        streamReader = new OpusReader();
      } else {
        return false;
      }
      return true;
    } catch (ParserException e) {
      return false;
    }
  }

  @Override
  public void init(ExtractorOutput output) {
    TrackOutput trackOutput = output.track(0);
    output.endTracks();
    streamReader.init(output, trackOutput);
  }

  @Override
  public void seek() {
    streamReader.seek();
  }

  @Override
  public void release() {
    // Do nothing
  }

  @Override
  public int read(ExtractorInput input, PositionHolder seekPosition)
      throws IOException, InterruptedException {
    return streamReader.read(input, seekPosition);
  }

  //@VisibleForTesting
  /* package */ StreamReader getStreamReader() {
    return streamReader;
  }

  private static ParsableByteArray resetPosition(ParsableByteArray scratch) {
    scratch.setPosition(0);
    return scratch;
  }

}
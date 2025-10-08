# Uploading larger files

Learn how to send files larger than 20 MB in multiple parts.

API bots in paid workspaces can use File Uploads in multi-part mode to upload files up to 5 GB. To do so, follow the steps below.

## Step 1: Split the file into parts

To send files larger than 20 MB, split them up into segments of 5-20 MB each. On Linux systems, one tool to do this is the [split command](https://phoenixnap.com/kb/linux-split). In other toolchains, there are libraries such as [split-file](https://github.com/tomvlk/node-split-file) for TypeScript to generate file parts.

```bash
# Split `largefile.txt` into 10MB chunks, named as follows:
# split_part_aa, split_part_ab, etc.
split -b 10M ./largefile.txt split_part
```

```typescript
import * as splitFile from "split-file";

const filename = "movie.MOV";
const inputFile = `${__dirname}/${filename}`;

// Returns an array of file paths in the current
// directory with a format of:
// [
//   "movie.MOV.sf-part1",
//   "movie.MOV.sf-part2",
//   ...
// ]
const outputFilenames = await splitFile.splitFileBySize(
  inputFile,
  1024 * 1024 * 10, // 10 MB
);
```

📘 **Convention for sizes of file parts**

When sending parts of a file to the Notion API, each file must be ≥ 5 and ≤ 20 (binary) megabytes in size, with the exception of the final part (the one with the highest part number), which can be less than 5 MB. The `split` command respects this convention, but the tools in your tech stack might vary. To stay within the range, we recommend using a part size of 10 MB.

## Step 2: Start a file upload

This is similar to [Step 1 of uploading small files](/reference/uploading-small-files#step-1), but with a few additional required parameters.

Pass a `mode` of `"multi_part"` to the [Create a file upload](/reference/create-a-file-upload) API, along with the `number_of_parts`, and a `filename` with a valid extension or a separate MIME `content_type` parameter that can be used to detect an extension.

```bash
curl --request POST \
  --url 'https://api.notion.com/v1/file_uploads' \
  -H 'Authorization: Bearer ntn_****' \
  -H 'Content-Type: application/json' \
  -H 'Notion-Version: <<latestNotionVersion>>' \
  --data '{
    "mode": "multi_part",
    "number_of_parts": 5,
    "filename": "image.png"
  }'
```

## Step 3: Send all file parts

Send each file part by using the [Send File Upload API](/reference/send-a-file-upload) using the File Upload ID, or the `upload_url` in the response of the [Create a file upload](/reference/create-a-file-upload) step.

This is similar to [Step 2 of uploading small files](/reference/uploading-small-files#step-2). However, alongside the `file`, the form data in your request must include a field `part_number` that identifies which part you're sending.

Your system can send file parts in parallel (up to standard Notion API [rate limits](/reference/request-limits)). Parts can be uploaded in any order, as long as the entire sequence from {1, …, `number_of_parts`} is successfully sent before calling the [Complete a file upload](/reference/complete-a-file-upload) API.

## Step 4: Complete the file upload

Call the [Complete a file upload](/reference/complete-a-file-upload) API with the ID of the File Upload after all parts are sent.

## Step 5: Attach the file upload

After completing the File Upload, its status becomes `uploaded` and it can be attached to blocks and other objects the same way as file uploads created with a `mode` of `single_part` (the default setting).

Using its ID, attach the File Upload (for example, to a block, page, or database) within one hour of creating it to avoid expiry.

📘 **Error handling**

The [Send](/reference/send-a-file-upload) API validates the total file size against the [workspace's limit](/docs/working-with-files-and-media#file-size-limits) at the time of uploading each part. However, because parts can be sent at the same time, the [Complete](/reference/complete-a-file-upload) step re-validates the combined file size and can also return an HTTP 400 with a code of `validation_error`.

We recommend checking the file's size before creating the File Upload when possible. Otherwise, make sure your integration can handle excessive file size errors returned from both the Send and Complete APIs.

To manually test your integration, command-line tools like `head`, `dd`, and `split` can help generate file contents of a certain size and split them into 10 MB parts.

---

*Updated 3 months ago*

Learn how to simplify migrations and syncs into Notion by automating file uploads from external URLs: